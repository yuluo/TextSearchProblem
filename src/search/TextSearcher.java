package search;

import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.io.StringWriter;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.*;
import org.json.JSONArray;
import org.json.JSONObject;

public class TextSearcher {

  private TextTokenizer lexer;
  private String wordRegex = "[a-zA-Z0-9\']+";
  private int chunkSize = 1000;
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
  protected List<Integer> findIndex(String queryWord) {
    String lowerCaseQuery = queryWord.toLowerCase();
    List<Integer> indexes = new ArrayList<Integer>();
    for (int i = 0; i < this.tokens.size(); i++) {
      String token = this.tokens.get(i).toLowerCase();
      if (token.contains(lowerCaseQuery)) {
        indexes.add(i);
      }
    }

    return indexes;
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
  public String[] search(String queryWord, int contextWords) throws Exception {
    // Check cache first
    List<Integer> indexes = new ArrayList<Integer>();
    if (cache.has(queryWord)) {
      JSONArray indexArray = cache.getJSONArray(queryWord);
      for (int i = 0; i < indexArray.length(); i++) {
        indexes.add(indexArray.getInt(i));
      }
    } else {
      // indexes = findIndex(queryWord);

      int numOfThreads = (int) Math.ceil((double) this.tokens.size() / this.chunkSize);
      System.out.println(numOfThreads);

      ExecutorService es = Executors.newCachedThreadPool();
      int start = 0;
      for (int i = 0; i < numOfThreads; i++) {
        es.execute(new TextSearchThread(tokens, indexes, queryWord, start, start + this.chunkSize));
        start += this.chunkSize;
      }
      es.shutdown();
      es.awaitTermination(1, TimeUnit.MINUTES);

      JSONArray indexArray = new JSONArray();
      for (int index : indexes) {
        indexArray.put(index);
      }
      cache.put(queryWord, indexArray);
    }
    System.out.println("Indexes");
    List<String> results = new ArrayList<String>();
    for (int index : indexes) {
      System.out.println(index);
      String context =
          getContextWords(index, contextWords * -1)
              + this.tokens.get(index)
              + getContextWords(index, contextWords);
      results.add(context);
    }

    return (String[]) results.toArray(new String[results.size()]);
  }

  public static void main(String[] args) throws Exception {
    File file = new File("files/long_excerpt.txt");
    TextSearcher searcher = new TextSearcher(file);

    long startTime = System.nanoTime();
    String[] results = searcher.search("geological", 3);
    long endTime = System.nanoTime();
    long duration = (endTime - startTime);
    System.out.println("duration: " + duration);

    for (String result : results) {
      System.out.println(result);
    }
  }
}

// Any needed utility classes can just go in this file
