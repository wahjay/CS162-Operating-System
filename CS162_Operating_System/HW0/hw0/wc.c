#include <stdio.h>

int readfile (char* filename, int* lineCount, int* wordCount, int* byte) {

    int ch;
    FILE *file = fopen(filename, "r");

       if (file) {

         ch = getc(file);
         while(ch != EOF) {

           if (ch == '\n') {
             (*byte)++;
             (*lineCount)++;
             ch = getc(file);

             while (ch == ' ' || ch == '\t') {
                (*byte)++;
                ch = getc(file);
             }
           } 

           else if (ch == ' ' || ch == '\t') {
             (*wordCount)++;

             while (ch == ' ' || ch == '\t') {
              (*byte)++;
              ch = getc(file);
             }
           }

           else if (ch != ' ' && ch != '\n') {
             (*byte)++;

             ch = getc(file);
             if (ch == '\n' || ch == EOF)
                (*wordCount)++;
           }
         }

        fclose(file);
	return 1;
       }

       else
	 return 0; 
}


int main(int argc, char *argv[]) {

    int lineCount = 0;
    int wordCount = 0;
    int byte = 0;
    int ch;

    if (argc == 1) {
	
      ch = getc(stdin);

      while(ch != EOF) {

           if (ch == '\n') {
             byte++;
             lineCount++;
             ch = getc(stdin);

             while (ch == ' ' || ch == '\t') {
                byte++;
                ch = getc(stdin);
             }
           }

           else if (ch == ' ' || ch == '\t' || ch == '\r' || ch == '\x00') {
            // wordCount++;

             while (ch == ' ' || ch == '\t' || ch == '\r' || ch == '\x00') {
              byte++;
              ch = getc(stdin);
             }
           }

           else if (ch != ' ' && ch != '\n') {
             byte++;

             ch = getc(stdin);
             if (ch == '\n' || ch == EOF || ch == ' ' || ch == '\t')  
                wordCount++;
           }

      }
      printf("%d %d %d \n", lineCount, wordCount, byte);
    }

    else if (argc == 2) {
 
       if (readfile(argv[1], &lineCount, &wordCount, &byte))    
       	 printf("%d %d %d %s \n", lineCount, wordCount, byte, argv[1]);

       else
	 printf("./wc: %s: open: No such file or directory\n", argv[1]);
    }

    else {

      int totalLines = 0;
      int totalWords = 0;
      int totalBytes = 0;
      int i;

      for(i=0; i<argc-1; i++) {
	
	if (readfile(argv[i+1], &lineCount, &wordCount, &byte)) {
	    
           totalLines += lineCount;
	   totalWords += wordCount;
	   totalBytes += byte; 
	   printf("%d %d %d %s \n", lineCount, wordCount, byte, argv[i+1]);
	   lineCount = 0;
	   wordCount = 0;
	   byte = 0;
	}
        
        else
	   printf("./wc: %s: open: No such file or directory\n", argv[i+1]);
      }
	
      printf("%d %d %d total \n", totalLines, totalWords, totalBytes);	 	
    } 
  
    return 0;
}
