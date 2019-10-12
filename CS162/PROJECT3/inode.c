#include "filesys/inode.h"
#include <list.h>
#include <debug.h>
#include <round.h>
#include <string.h>
#include "filesys/filesys.h"
#include "filesys/free-map.h"
#include "threads/malloc.h"
#include "threads/synch.h"
#include "filesys/cache.h"

/* Identifies an inode. */
#define INODE_MAGIC 0x494e4f44

/* Number of pointers per block (512/4) */
#define BLOCK_PTRS 128

/* Inode_disk struct: number of direct pointers */
#define NUM_DIRECTS 13
#define NUM_INDIRECTS 1 // may change later.

/* On-disk inode.
 Must be exactly BLOCK_SECTOR_SIZE bytes long. */
struct inode_disk
{
    off_t length;                         /* File size in bytes. */
    unsigned magic;                       /* Magic number. */
    uint32_t unused[108];                 /* Not used. */
    bool is_dir;                          /* Directory or not. */
    block_sector_t parent;
    
    size_t num_sectors;                   /* Numer of sectors used. */
    block_sector_t directs[NUM_DIRECTS];
    block_sector_t indirect;
    block_sector_t double_indirect;
};

/* Returns the number of sectors to allocate for an inode SIZE
 bytes long. */
static inline size_t
bytes_to_sectors (off_t size)
{
    return DIV_ROUND_UP (size, BLOCK_SECTOR_SIZE);
}

/* In-memory inode. */
struct inode
{
    struct list_elem elem;              /* Element in inode list. */
    block_sector_t sector;              /* Sector number of disk location. */
    int open_cnt;                       /* Number of openers. */
    bool removed;                       /* True if deleted, false otherwise. */
    int deny_write_cnt;                 /* 0: writes ok, >0: deny writes. */
    
    off_t length;                       /* File size in bytes. */
    bool is_dir;
    block_sector_t parent;
    struct lock lock;
    
    size_t num_sectors;
    block_sector_t directs[NUM_DIRECTS];
    block_sector_t indirect;
    block_sector_t double_indirect;
};

static void inode_allocate (struct inode_disk *inode_disk);
static bool inode_grow (struct inode* inode,block_sector_t sectors);
static void inode_free (struct inode *inode);


/* Returns the block device sector that contains byte offset POS
 within INODE.
 Returns -1 if INODE does not contain data for a byte at offset
 POS. */
static block_sector_t
byte_to_sector (const struct inode *inode, off_t pos)
{
    ASSERT (inode != NULL);
    if (pos < inode_length(inode))
    {
        size_t num_blocks = pos / BLOCK_SECTOR_SIZE;
        
        // Within direct blocks
        if(num_blocks < NUM_DIRECTS )
            return inode->directs[num_blocks];
        
        // Lies in indrect blocks
        else if (num_blocks < NUM_DIRECTS + BLOCK_PTRS * NUM_INDIRECTS)
        {
            block_sector_t blocks[BLOCK_PTRS];
            off_t offset = num_blocks - NUM_DIRECTS;
            
            // Get the index of the internal block.
            off_t index = offset % BLOCK_PTRS;
            block_read(fs_device, inode->indirect, &blocks);
            return blocks[index];
        }
        
        // Lies in double indirect blocks
        else if (num_blocks < NUM_DIRECTS+ BLOCK_PTRS + BLOCK_PTRS * BLOCK_PTRS )
        {
            off_t offset = num_blocks - NUM_DIRECTS - BLOCK_PTRS;
            
            // Get the position of the wanted block in the internal block.
            off_t index = offset / BLOCK_PTRS;
            
            // Get the first level internal block.
            block_sector_t indirect_blocks[BLOCK_PTRS];
            block_read(fs_device, inode->double_indirect, &indirect_blocks);
            
            // Get the second level internal block.
            block_sector_t double_indirect_blocks[BLOCK_PTRS];
            block_read(fs_device, indirect_blocks[index], &double_indirect_blocks);
            
            // Get the position of the wanted block in the second level internal block.
            index = offset % BLOCK_PTRS;
            return double_indirect_blocks[index];
        }
    }
    return -1;
}

/* List of open inodes, so that opening a single inode twice
 returns the same `struct inode'. */
static struct list open_inodes;

/* Initializes the inode module. */
void
inode_init (void)
{
    list_init (&open_inodes);
}

/* Initializes an inode with LENGTH bytes of data and
 writes the new inode to sector SECTOR on the file system
 device.
 Returns true if successful.
 Returns false if memory or disk allocation fails. */
bool
inode_create (block_sector_t sector, off_t length)
{
    struct inode_disk *disk_inode = NULL;
    bool success = false;
    
    ASSERT (length >= 0);
    
    /* If this assertion fails, the inode structure is not exactly
     one sector in size, and you should fix that. */
    ASSERT (sizeof *disk_inode == BLOCK_SECTOR_SIZE);
    
    disk_inode = calloc (1, sizeof *disk_inode);
    if (disk_inode != NULL)
    {
        disk_inode->length = length;
        disk_inode->magic = INODE_MAGIC;
        disk_inode->is_dir = false;         // Always false for now.
        disk_inode->parent = ROOT_DIR_SECTOR;
        inode_allocate (disk_inode);
        block_write (fs_device, sector, disk_inode);
        success = true;
        free (disk_inode);
    }
    return success;
}

/* Reads an inode from SECTOR
 and returns a `struct inode' that contains it.
 Returns a null pointer if memory allocation fails. */
struct inode *
inode_open (block_sector_t sector)
{
    struct list_elem *e;
    struct inode *inode;
    struct inode_disk inode_disk;
    
    /* Check whether this inode is already open. */
    for (e = list_begin (&open_inodes); e != list_end (&open_inodes); e = list_next (e))
    {
        inode = list_entry (e, struct inode, elem);
        if (inode->sector == sector)
        {
            inode_reopen (inode);
            return inode;
        }
    }
    
    /* Allocate memory. */
    inode = malloc (sizeof *inode);
    if (inode == NULL)
        return NULL;
    
    /* Initialize. */
    list_push_front (&open_inodes, &inode->elem);
    inode->sector = sector;
    inode->open_cnt = 1;
    inode->deny_write_cnt = 0;
    inode->removed = false;
    lock_init(&inode->lock);
    
    // Copy INODE_DISK to INODE.
    block_read(fs_device, inode->sector, &inode_disk);
    inode->length = inode_disk.length;
    inode->num_sectors = inode_disk.num_sectors;
    inode->is_dir = inode_disk.is_dir;
    inode->parent = inode_disk.parent;
    memcpy(&inode->directs, &inode_disk.directs, NUM_DIRECTS * sizeof(block_sector_t));
    memcpy(&inode->indirect, &inode_disk.indirect, NUM_INDIRECTS * sizeof(block_sector_t));
    memcpy(&inode->double_indirect, &inode_disk.double_indirect, sizeof(block_sector_t));
    return inode;
}

/* Reopens and returns INODE. */
struct inode *
inode_reopen (struct inode *inode)
{
    if (inode != NULL)
        inode->open_cnt++;
    return inode;
}

/* Returns INODE's inode number. */
block_sector_t
inode_get_inumber (const struct inode *inode)
{
    return inode->sector;
}

/* Closes INODE and writes it to disk.
 If this was the last reference to INODE, frees its memory.
 If INODE was also a removed inode, frees its blocks. */
void
inode_close (struct inode *inode)
{
    struct inode_disk *inode_disk = calloc (1, sizeof *inode_disk);
    /* Ignore null pointer. */
    if (inode == NULL)
        return;
    
    /* Release resources if this was the last opener. */
    if (--inode->open_cnt == 0)
    {
        /* Remove from inode list and release lock. */
        list_remove (&inode->elem);
        
        /* Deallocate blocks if removed. */
        if (inode->removed)
        {
            free_map_release (inode->sector, 1);
            inode_free(inode);
        }
        else
        {   /* Write back before close. */
            inode_disk->length = inode->length;
            inode_disk->magic = INODE_MAGIC;
            inode_disk->num_sectors = inode->num_sectors;
            inode_disk->is_dir = inode->is_dir;
            inode_disk->parent = inode->parent;
            memcpy(&inode_disk->directs, &inode->directs, NUM_DIRECTS * sizeof(block_sector_t));
            memcpy(&inode_disk->indirect, &inode->indirect, NUM_INDIRECTS * sizeof(block_sector_t));
            memcpy(&inode_disk->double_indirect, &inode->double_indirect, sizeof(block_sector_t));
            block_write(fs_device, inode->sector, inode_disk);
        }
        free (inode);
        free(inode_disk);
    }
}

/* Marks INODE to be deleted when it is closed by the last caller who
 has it open. */
void
inode_remove (struct inode *inode)
{
    ASSERT (inode != NULL);
    inode->removed = true;
}

/* Reads SIZE bytes from INODE into BUFFER, starting at position OFFSET.
 Returns the number of bytes actually read, which may be less
 than SIZE if an error occurs or end of file is reached. */
off_t
inode_read_at (struct inode *inode, void *buffer_, off_t size, off_t offset)
{
    uint8_t *buffer = buffer_;
    off_t bytes_read = 0;
    uint8_t *bounce = NULL;
    
    if(offset >= inode->length)
        return 0;
    
    while (size > 0)
    {
        /* Disk sector to read, starting byte offset within sector. */
        block_sector_t sector_idx = byte_to_sector (inode, offset);
        int sector_ofs = offset % BLOCK_SECTOR_SIZE;
        
        /* Bytes left in inode, bytes left in sector, lesser of the two. */
        off_t inode_left = inode_length (inode) - offset;
        int sector_left = BLOCK_SECTOR_SIZE - sector_ofs;
        int min_left = inode_left < sector_left ? inode_left : sector_left;
        
        /* Number of bytes to actually copy out of this sector. */
        int chunk_size = size < min_left ? size : min_left;
        if (chunk_size <= 0)
            break;
        
        if (sector_ofs == 0 && chunk_size == BLOCK_SECTOR_SIZE)
        {
            // Read full sector directly into caller's buffer.
            block_read (fs_device, sector_idx, buffer + bytes_read);
        }
        else
        {
            /* Read sector into bounce buffer, then partially copy
             into caller's buffer. */
            if (bounce == NULL)
            {
                bounce = malloc (BLOCK_SECTOR_SIZE);
                if (bounce == NULL)
                    break;
            }
            
            block_read (fs_device, sector_idx, bounce);
            memcpy (buffer + bytes_read, bounce + sector_ofs, chunk_size);
        }
        
        /* Advance. */
        size -= chunk_size;
        offset += chunk_size;
        bytes_read += chunk_size;
    }
    free (bounce);
    return bytes_read;
}

/* Writes SIZE bytes from BUFFER into INODE, starting at OFFSET.
 Returns the number of bytes actually written, which may be
 less than SIZE if end of file is reached or an error occurs. */
off_t
inode_write_at (struct inode *inode, const void *buffer_, off_t size, off_t offset)
{
    const uint8_t *buffer = buffer_;
    off_t bytes_written = 0;
    uint8_t *bounce = NULL;
    
    if (inode->deny_write_cnt)
        return 0;
    
    /* Extend the file. */
    if(offset + size > inode->length)
    {
        inode_grow(inode, bytes_to_sectors(offset + size));
        inode->length = offset + size;
    }
    
    
    while (size > 0)
    {
        /* Sector to write, starting byte offset within sector. */
        block_sector_t sector_idx = byte_to_sector (inode, offset);
        int sector_ofs = offset % BLOCK_SECTOR_SIZE;
        
        /* Bytes left in inode, bytes left in sector, lesser of the two. */
        off_t inode_left = inode_length (inode) - offset;
        int sector_left = BLOCK_SECTOR_SIZE - sector_ofs;
        int min_left = inode_left < sector_left ? inode_left : sector_left;
        
        /* Number of bytes to actually write into this sector. */
        int chunk_size = size < min_left ? size : min_left;
        if (chunk_size <= 0)
            break;
        
        if (sector_ofs == 0 && chunk_size == BLOCK_SECTOR_SIZE)
        {
            // Write full sector directly to disk.
            block_write (fs_device, sector_idx, buffer + bytes_written);
        }
        else
        {
            // We need a bounce buffer.
            if (bounce == NULL)
            {
                bounce = malloc (BLOCK_SECTOR_SIZE);
                if (bounce == NULL)
                    break;
            }
            
            /* If the sector contains data before or after the chunk
             we're writing, then we need to read in the sector
             first.  Otherwise we start with a sector of all zeros. */
            if (sector_ofs > 0 || chunk_size < sector_left)
                block_read (fs_device, sector_idx, bounce);
            else
                memset (bounce, 0, BLOCK_SECTOR_SIZE);
            memcpy (bounce + sector_ofs, buffer + bytes_written, chunk_size);
            block_write (fs_device, sector_idx, bounce);
        }
        
        /* Advance. */
        size -= chunk_size;
        offset += chunk_size;
        bytes_written += chunk_size;
    }
    free (bounce);
    return bytes_written;
}

/* Disables writes to INODE.
 May be called at most once per inode opener. */
void
inode_deny_write (struct inode *inode)
{
    inode->deny_write_cnt++;
    ASSERT (inode->deny_write_cnt <= inode->open_cnt);
}

/* Re-enables writes to INODE.
 Must be called once by each inode opener who has called
 inode_deny_write() on the inode, before closing the inode. */
void
inode_allow_write (struct inode *inode)
{
    ASSERT (inode->deny_write_cnt > 0);
    ASSERT (inode->deny_write_cnt <= inode->open_cnt);
    inode->deny_write_cnt--;
}

/* Returns the length, in bytes, of INODE's data. */
off_t
inode_length (const struct inode *inode)
{
    return inode->length;
}

/* Allocate and grow the INODE and copy to its corresponding INODE_DISK. */
static void
inode_allocate (struct inode_disk *inode_disk)
{
    struct inode inode;
    inode.num_sectors = 0;
    inode.length = 0;
    
    inode_grow(&inode, bytes_to_sectors(inode_disk->length));
    
    inode_disk->num_sectors = inode.num_sectors;
    memcpy(&inode_disk->directs, &inode.directs, NUM_DIRECTS * sizeof(block_sector_t));
    memcpy(&inode_disk->indirect, &inode.indirect, NUM_INDIRECTS * sizeof(block_sector_t));
    memcpy(&inode_disk->double_indirect, &inode.double_indirect, sizeof(block_sector_t));
}

static bool
inode_grow (struct inode *inode, block_sector_t sectors)
{
    static char zeros[BLOCK_SECTOR_SIZE];
    
    // Get the number of free sectors.
    size_t limit = sectors - bytes_to_sectors(inode->length);
    
    if (limit == 0)
        return false;
    
    // Direct blocks.
    while (inode->num_sectors < NUM_DIRECTS && limit > 0)
    {
        free_map_allocate (1, &inode->directs[inode->num_sectors]);
        block_write(fs_device, inode->directs[inode->num_sectors], zeros);
        inode->num_sectors++;
        limit--;
    }
    
    
    // Indirect blocks.
    if (inode->num_sectors < NUM_DIRECTS + BLOCK_PTRS * NUM_INDIRECTS && limit > 0)
    {
        block_sector_t blocks[BLOCK_PTRS];
        off_t indirect_index = (inode->num_sectors - NUM_DIRECTS) % BLOCK_PTRS;
        
        if (indirect_index == 0)
            free_map_allocate(1, &inode->indirect);
        
        else
            block_read(fs_device, inode->indirect, &blocks);
        
        while (indirect_index < BLOCK_PTRS && limit > 0)
        {
            free_map_allocate(1, &blocks[indirect_index]);
            block_write(fs_device, blocks[indirect_index], zeros);
            indirect_index++;
            inode->num_sectors++;
            limit--;
        }
        
        // Write back to indirect block.
        block_write(fs_device, inode->indirect, &blocks);
        
        // Increment to next indirect block.
        /*
         if(indirect_index == BLOCK_PTRS)
         {
         indirect_index = 0;
         index++;
         }
         */
    }
    
    // double indirect blocks
    if (inode->num_sectors < NUM_DIRECTS + BLOCK_PTRS + BLOCK_PTRS * BLOCK_PTRS
        && limit > 0)
    {
        block_sector_t level_one[BLOCK_PTRS];
        block_sector_t level_two[BLOCK_PTRS];
        
        off_t offset = inode->num_sectors - NUM_DIRECTS - BLOCK_PTRS;
        size_t indirect_index = offset / BLOCK_PTRS;  // level 1
        size_t double_indirect_index = offset % BLOCK_PTRS; // level 2
        
        // read up first level block
        if (indirect_index == 0 && double_indirect_index == 0)
            free_map_allocate(1, &inode->double_indirect);
        
        else
            block_read(fs_device, inode->double_indirect, &level_one);
        
        // Zeroing out the alloccated blocks in the first level.
        while (indirect_index < BLOCK_PTRS && limit > 0)
        {
            // read up second level block
            if (double_indirect_index == 0)
                free_map_allocate(1, &level_one[indirect_index]);
            
            else
                block_read(fs_device, level_one[indirect_index], &level_two);
            
            // Zeroing out the alloccated blocks in the second level.
            while (double_indirect_index < BLOCK_PTRS && limit > 0)
            {
                free_map_allocate(1, &level_two[double_indirect_index]);
                block_write(fs_device, level_two[double_indirect_index], zeros);
                double_indirect_index++;
                inode->num_sectors++;
                limit--;
            }
            
            // Write back second level blocks.
            block_write(fs_device, level_one[indirect_index], &level_two);
            
            // Increment to the next block.
            if (double_indirect_index == BLOCK_PTRS)
            {
                double_indirect_index = 0;
                indirect_index++;
            }
        }
        
        // Write back first level blocks.
        block_write(fs_device, inode->double_indirect, &level_one);
    }
    return true;
}

static void
inode_free (struct inode *inode)
{
    // Get the total sectors used.
    size_t sectors = bytes_to_sectors(inode->length);
    size_t index = 0;
    
    if(sectors == 0)
        return;
    
    // Free direct blocks.
    while (index < NUM_DIRECTS && sectors > 0)
    {
        free_map_release (inode->directs[index], 1);
        sectors--;
        index++;
    }
    
    // Free indirect blocks.
    if (inode->num_sectors >= NUM_DIRECTS && sectors > 0)
    {
        // lesser of the two.
        size_t free_blocks = sectors < BLOCK_PTRS ?  sectors : BLOCK_PTRS;
        
        size_t i;
        block_sector_t block[BLOCK_PTRS];
        block_read(fs_device, inode->indirect, &block);
        
        
        for (i = 0; i < free_blocks; i++)
        {
            free_map_release(block[i], 1);
            sectors--;
        }
        
        free_map_release(inode->indirect, 1);
    }
    
    // Free double indirect blocks.
    if (inode->num_sectors >= NUM_DIRECTS + BLOCK_PTRS && sectors > 0)
    {
        block_sector_t indirect_blocks[BLOCK_PTRS];
        block_sector_t double_indirect_blocks[BLOCK_PTRS];
        
        // Get the first level internal block.
        block_read(fs_device, inode->double_indirect, &indirect_blocks);
        
        // Get the indirect index in the first level block.
        size_t indirect_index = (inode->num_sectors - NUM_DIRECTS - BLOCK_PTRS) / BLOCK_PTRS;
        
        size_t i, j;
        for (i = 0; i < indirect_index; i++)
        {
            // lesser of the two.
            size_t free_blocks = sectors < BLOCK_PTRS ? sectors : BLOCK_PTRS;
            
            // Get the second level internal block.
            block_read(fs_device, indirect_blocks[i], &double_indirect_blocks);
            
            for (j = 0; j < free_blocks; j++)
            {
                free_map_release(double_indirect_blocks[j], 1);
                sectors--;
            }
            
            // Free second level block.
            free_map_release(indirect_blocks[i], 1);
        }
        
        // Free first level block.
        free_map_release(inode->double_indirect, 1);
    }
}
