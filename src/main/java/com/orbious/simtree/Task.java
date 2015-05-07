package com.orbious.simtree;

import java.util.HashMap;
import java.util.concurrent.Callable;


public class Task implements Callable<HashMap<String, Integer>> {
  
  private String word;
  
  public Task(String word) { 
    this.word = word;
  }
  
  @Override
  public HashMap<String, Integer> call() throws Exception {
    // FIXME: do something with word
    return null;
  }
}
