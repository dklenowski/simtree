package com.orbious.simtree;

import java.util.List;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

import org.apache.log4j.ConsoleAppender;
import org.apache.log4j.Level;
import org.apache.log4j.Logger;
import org.apache.log4j.PatternLayout;

public class Main {
  
  public static final Logger log = Logger.getLogger("simtree");
  
  // words we have processed
  public static HashSet<String> processed = new HashSet<>();
  
  // an identifier for our clusters
  public static int cluster_ct = 0; 

  // key - cluster_ct
  // val - key -> word
  //       val -> depth
  public static HashMap<String, HashMap<String, Integer>> clusters = new HashMap<>();
  
  // system paraemets
  public static int thread_ct = 50;
  public static String dictionary_file = "/tmp/dict.txt";
  public static String output_file = "/tmp/simtree.txt";
  
  
  static {
    log.setLevel(Level.DEBUG);
    log.addAppender(new ConsoleAppender(
        new PatternLayout("%d{ISO8601} %-5p  %C{2} (%M:%L) - %m%n") ));
  }
  
  private static void usage() {
    System.out.println(
        "Usage: Main [-h] [-d <dict>]n" +
        "    -d <dict> Parse dictionary file <dict>"); 
    System.exit(1);
  }

  private static ArrayList<String> read(String name) throws IOException {
    ArrayList<String> dict = new ArrayList<>();
    
    BufferedReader reader = new BufferedReader(new FileReader(new File(name)));
    String line;
    while ( (line = reader.readLine()) != null ) 
      dict.add(line);

    reader.close();
    log.debug("read " + dict.size() + " entries from " + name);
    return dict;
  }
  
  public static void main(String[] args) throws IOException {
    ArrayList<String> dict = read(dictionary_file);
    
    ExecutorService executor = Executors.newFixedThreadPool(thread_ct);
    int ct = 0;
    String word;
    while ( ct < dict.size() ) {
      ArrayList<Task> tasks = new ArrayList<>();
      while ( true ) {
        word = dict.get(ct++);
        if ( processed.contains(word) ) {
          log.info("already processed " + word);
          continue;
        }
        
        tasks.add(new Task(word));
        if ( tasks.size() >= thread_ct || ct >= dict.size() ) break;
      }

      List<Future<HashMap<String, Integer>>> futures = null;
      try {
        futures = executor.invokeAll(tasks);
      } catch ( InterruptedException ie ) {
        log.fatal("interrupted exception during invokeAll", ie);
        Thread.currentThread().interrupt();
      }
      
      if ( futures == null ) {
        log.fatal("failed to get any results?");
        continue;
      }
      
      for ( Future<HashMap<String, Integer>> future : futures ) {
        HashMap<String, Integer> hm = null;
        try {
          hm = future.get();
        } catch ( InterruptedException ie ) {
          log.fatal("interrupted exception during future#get");
          Thread.currentThread().interrupt();
        } catch ( ExecutionException ee ) {
          log.fatal("bailing out?", ee);
          System.exit(1);
        }
        
        clusters.put(String.valueOf(cluster_ct), hm);
        for ( String key: hm.keySet() ) 
          processed.add(key);
      }
      
    }

    executor.shutdown();
    while ( true ) {
      try {
        executor.awaitTermination(5, TimeUnit.MINUTES);
      } catch ( InterruptedException ignored ) { }

      if ( executor.isTerminated() ) {
        log.info("executor finished!");
        break;
      }
    }
  }

}
