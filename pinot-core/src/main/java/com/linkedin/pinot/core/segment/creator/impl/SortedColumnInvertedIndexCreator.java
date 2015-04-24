package com.linkedin.pinot.core.segment.creator.impl;

import java.io.File;
import java.io.IOException;
import java.util.Arrays;

import com.linkedin.pinot.common.data.FieldSpec;
import com.linkedin.pinot.core.index.writer.impl.FixedByteWidthRowColDataFileWriter;
import com.linkedin.pinot.core.segment.creator.InvertedIndexCreator;


public class SortedColumnInvertedIndexCreator implements InvertedIndexCreator {
  private FixedByteWidthRowColDataFileWriter indexWriter;
  private int[] mins;
  private int[] maxs;
  private long start = 0;
  private int cardinality;

  public SortedColumnInvertedIndexCreator(File indexDir, int cardinality, FieldSpec spec) throws Exception {
    File indexFile = new File(indexDir, spec.getName() + V1Constants.Indexes.SORTED_INVERTED_INDEX_FILE_EXTENSION);
    indexWriter = new FixedByteWidthRowColDataFileWriter(indexFile, cardinality, 2, new int[] { 4, 4 });
    mins = new int[cardinality];
    maxs = new int[cardinality];

    for (int i = 0; i < mins.length; i++) {
      mins[i] = Integer.MAX_VALUE;
    }
    for (int i = 0; i < maxs.length; i++) {
      maxs[i] = -1;
    }
    start = System.currentTimeMillis();
    this.cardinality = cardinality;
  }

  @Override
  public void add(int dictionaryId, int docId) {

    if (mins[dictionaryId] > docId) {
      mins[dictionaryId] = docId;
    }
    if (maxs[dictionaryId] < docId) {
      maxs[dictionaryId] = docId;
    }
  }

  @Override
  public void add(Object dictionaryIds, int docId) {
    if (dictionaryIds instanceof Integer) {
      int dictionaryId = ((Integer) dictionaryIds).intValue();
      add(dictionaryId, docId);
      return;
    }

    final Integer[] entryArr = ((Integer[]) dictionaryIds);
    Arrays.sort(entryArr);

    for (int i = 0; i < entryArr.length; i++) {
      add(entryArr[i], docId);
    }
  }

  @Override
  public long totalTimeTakeSoFar() {
    return (System.currentTimeMillis() - start);
  }

  @Override
  public void seal() throws IOException {
    for (int i = 0; i < cardinality; i++) {
      indexWriter.setInt(i, 0, mins[i]);
      indexWriter.setInt(i, 1, maxs[i]);
    }

    indexWriter.close();
  }

}