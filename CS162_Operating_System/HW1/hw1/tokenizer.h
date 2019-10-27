#pragma once

/* A struct that represents a list of words. */
struct tokens;

/* Turn a string into a list of words. */
struct tokens *tokenize(const char *line);

/* How many words are there? */
size_t tokens_get_length(struct tokens *tokens);

/* Get me the Nth word (zero-indexed) */
char *tokens_get_token(struct tokens *tokens, size_t n);

/* Free the memory */
void tokens_destroy(struct tokens *tokens);

/* Put these functions here so shell.c can access it. */
void *vector_push(char ***pointer, size_t *size, void *elem);
void *copy_word(char *source, size_t n);
