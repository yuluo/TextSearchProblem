package search;

import java.util.List;

class TextSearchThread implements Runnable {
  private List<String> tokens;
  private List<Integer> indexes;
  private String queryWord;
  private int start;
  private int end;

  public TextSearchThread(
      List<String> tokens, List<Integer> indexes, String queryWord, int start, int end) {
    this.tokens = tokens;
    this.indexes = indexes;
    this.queryWord = queryWord;
    this.start = start;
    this.end = end;
  }

  public void run() {
    String lowerCaseQuery = queryWord.toLowerCase();
    for (int i = start; i < end; i++) {
      if (i >= this.tokens.size()) {
        break;
      } else {
        String token = this.tokens.get(i).toLowerCase();
        if (token.contains(lowerCaseQuery)) {
          indexes.add(i);
        }
      }
    }
  }
}
