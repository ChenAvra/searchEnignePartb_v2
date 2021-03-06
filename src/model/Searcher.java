package model;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.TreeMap;

public class Searcher {
    private Ranker r;


    public Searcher(boolean toUseSemantic) {
        r=new Ranker(toUseSemantic);
    }

    public HashMap<String,Map<String,Double>>   RankDocs(String nameQuery, String query, Indexer index, String pathToWrite, HashMap<String, Map<String,Double>> relevantDoc, double docAvg, boolean toStem, boolean onLine) throws Exception {
        //to add parsing option

        return  r.collectLinesQuery(nameQuery, query, index, pathToWrite, relevantDoc, docAvg,toStem,onLine);


    }
}
