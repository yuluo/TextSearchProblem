package search;

import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.io.StringWriter;
import java.util.ArrayList;
import java.util.List;
import org.json.JSONArray;
import org.json.JSONObject;

public class TextSearcher {

  private TextTokenizer lexer;
  private String wordRegex = "[a-zA-Z0-9\']+";
  private List<String> tokens;
  /** Cache structure { "queryWord1": [10, 32] } */
  private JSONObject cache;

  /**
   * Initializes the text searcher with the contents of a text file. The current implementation just
   * reads the contents into a string and passes them to #init(). You may modify this implementation
   * if you need to.
   *
   * @param f Input file.
   * @throws IOException
   */
  public TextSearcher(File f) throws IOException {
    FileReader r = new FileReader(f);
    StringWriter w = new StringWriter();
    char[] buf = new char[4096];
    int readCount;

    while ((readCount = r.read(buf)) > 0) {
      w.write(buf, 0, readCount);
    }

    // read config file here

    init(w.toString());
  }

  /**
   * Initializes any internal data structures that are needed for this class to implement search
   * efficiently.
   */
  protected void init(String fileContents) {
    // TODO -- fill in implementation
    this.lexer = new TextTokenizer(fileContents, wordRegex);
    this.tokens = new ArrayList<String>();
    this.cache = new JSONObject();

    while (this.lexer.hasNext()) {
      this.tokens.add(lexer.next());
    }
  }

  /** find all the index of the queryword */
  protected Integer[] findIndex(String queryWord) {
    String lowerCaseQuery = queryWord.toLowerCase();
    List<Integer> indexes = new ArrayList<Integer>();
    for (int i = 0; i < this.tokens.size(); i++) {
      String token = this.tokens.get(i).toLowerCase();
      if (token.contains(lowerCaseQuery)) {
        indexes.add(i);
      }
    }

    return (Integer[]) indexes.toArray(new Integer[indexes.size()]);
  }

  protected String getContextWords(int index, int num) {
    int numOfContext = 0;
    int direction = num > 0 ? 1 : -1;
    int start = index + direction;
    String context = "";

    while (numOfContext != Math.abs(num)) {
      String token = this.tokens.get(start);
      if (this.lexer.isWord(token)) {
        numOfContext++;
      }

      if (num > 0) {
        context += token;
      } else {
        context = token + context;
      }
      start += direction;

      if (start < 0 || start == this.tokens.size()) {
        numOfContext = Math.abs(num);
      }
    }

    return context;
  }

  /**
   * @param queryWord The word to search for in the file contents.
   * @param contextWords The number of words of context to provide on each side of the query word.
   * @return One context string for each time the query word appears in the file.
   */
  public String[] search(String queryWord, int contextWords) {
    // Check cache first
    Integer[] indexes;
    if (cache.has(queryWord)) {
      JSONArray indexArray = cache.getJSONArray(queryWord);
      indexes = new Integer[indexArray.length()];
      for (int i = 0; i < indexArray.length(); i++) {
        indexes[i] = indexArray.getInt(i);
      }
    } else {
      indexes = findIndex(queryWord);
      JSONArray indexArray = new JSONArray();
      for (int index : indexes) {
        indexArray.put(index);
      }
      cache.put(queryWord, indexArray);
    }

    List<String> results = new ArrayList<String>();
    for (int index : indexes) {
      String context =
          getContextWords(index, contextWords * -1)
              + this.tokens.get(index)
              + getContextWords(index, contextWords);
      results.add(context);
    }

    return (String[]) results.toArray(new String[results.size()]);
  }

  /*
  public static void main(String[] args) throws Exception {
    File file = new File("files/short_excerpt.txt");
    TextSearcher searcher = new TextSearcher(file);
    String[] results = searcher.search("Naturalists", 1);
    results = searcher.search("Naturalists", 2);
    for (String result : results) {
      System.out.println(result);
    }

    System.out.println("end");
  }
  */
}

// Any needed utility classes can just go in this file
