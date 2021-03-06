package model;

import com.fasterxml.jackson.databind.ObjectMapper;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.*;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.medallia.word2vec.Searcher;
import com.medallia.word2vec.Word2VecModel;
import javafx.util.Pair;
import org.tartarus.snowball.ext.PorterStemmer;

import java.io.IOException;
import java.util.ArrayList;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.HashMap;
import java.util.TreeMap;

public class Ranker {

    private HashMap<String, Double> weights;
    private boolean toUseSemantic;
    private String query;
    private final String USER_AGENT = "Mozilla/5.0";
    PorterStemmer stemmer;
    public Ranker(boolean toUseSemantic) {
        weights=new HashMap<>();
        this.toUseSemantic=toUseSemantic;
        this.stemmer=new PorterStemmer();
    }


    public HashMap<String,Map<String, Double>> collectLinesQuery(String nameQuery, String initialQuery, Indexer index,String pathToWrite,HashMap<String,Map<String,Double>> relevantDoc, double docAvg, boolean toStem, boolean onLine) throws Exception {
        if(toStem){
            String fixedInitialQuery="";
            String [] s = initialQuery.split(" ");
            for(int i=0; i<s.length; i++){
                String d= s[i];
                stemmer.setCurrent(d); //set string you need to stem
                stemmer.stem();  //stem the word
                fixedInitialQuery=fixedInitialQuery+stemmer.getCurrent()+" ";//get the stemmed word

            }
            query=semantic(fixedInitialQuery,onLine,toStem);//stemmed query
        }
        else{
            query=semantic(initialQuery,onLine,toStem);
        }
        //query=semantic(initialQuery);
        String[] splitedQuery=query.split(" ");
        int  numOfDOC=index.getP().getDocInfo().size();
        int sumTotalIdf=0;
        File file3;
       if(toStem){
          file3= new File(pathToWrite+"\\"+index.getPostingFileName_WithStem());
       }
       else{
           file3= new File(pathToWrite+"\\"+index.getPostingFileName_NoStem());
       }

        String st="";
        String [] splitedPosting;


        BufferedReader posting= new BufferedReader(new FileReader(file3));

        String investigate="";

        int sumIdFwordQuery=0;
        int countDoc=0;
        st=posting.readLine();
        while (st!=null) {
                for (int i = 0; i < splitedQuery.length; i++) {
                    splitedPosting = st.split("@");
//                    if(toStem){
//                        if(!splitedQuery[i].equals("")){
//                            String d= splitedQuery[i];
//                            stemmer.setCurrent(d); //set string you need to stem
//                            stemmer.stem();  //stem the word
//                            splitedQuery[i]= stemmer.getCurrent();//get the stemmed word
//                        }
//                    }
                    if (splitedPosting[0].toLowerCase().equals(splitedQuery[i].toLowerCase())) {
                        investigate=investigate+st;
                        BN25(nameQuery,investigate,relevantDoc,numOfDOC,index.getP().getDocInfo(),docAvg);

                    }
                    investigate="";

                }
                st=posting.readLine();
            }
//        for(Map.Entry<String,Map<String,Double>> entry : relevantDoc.entrySet() ){
//            Map<String,Double> needSorted=entry.getValue();
//            List<Double>  need = new ArrayList<>(needSorted.values());
//            Collections.sort(need);
//
//
//
//        }
        return relevantDoc;
        }




    public void BN25(String nameQuery,String postingLine, HashMap<String, Map<String,Double>> relevantDoc,int numOfDoc,HashMap<String, DocDetails> DocInfo,double avg){
        String []st=postingLine.split("@");
        String []docAppear=st[2].split(" ");
        for(int i=0; i<docAppear.length; i=i+2){
            String docName=docAppear[i];
            int numOfAppear=Integer.parseInt(docAppear[i+1]);
            //compute tf
          //  double tf=(double) numOfAppear/(DocInfo.get(docName).getMax_tf());
            //try 2 more options
            double tf=(double) numOfAppear;

            double numOfDocCurrenQuery=docAppear.length/2;

            //conpute idf
            double idf=Math.log((numOfDoc-numOfDocCurrenQuery+0.5)/(numOfDocCurrenQuery+0.5));
            //try one more option from class

            //compute score for qi
//            if(!DocInfo.containsKey(docName)){
//                System.out.println("i don't have docName:"+docName);
//            }
            double partBScore= (tf*(1.3+1))/((1-0.75+0.75*(DocInfo.get(docName).getDocSize()/avg))+tf);

//            if(!weights.containsKey(st[0].toLowerCase())){
//                System.out.println("i don't have word "+st[0]+" in weights hashMap");
//                System.out.println("i'm in doc:"+docName);
//            }
            double score=(weights.get(st[0].toLowerCase()))*idf*partBScore;

            if(relevantDoc.containsKey(nameQuery)){
               if(relevantDoc.get(nameQuery).containsKey(docName)){
                  double scoreToAdd= relevantDoc.get(nameQuery).get(docName)+score;
                   relevantDoc.get(nameQuery).replace(docName,scoreToAdd);
               }
               else{
                   relevantDoc.get(nameQuery).put(docName,score);
               }

            }
            else{
                HashMap<String,Double> toAdd=new HashMap<>();
                toAdd.put(docName,score);
                relevantDoc.put(nameQuery,toAdd);
            }
        }





    }
    public String semantic(String query,boolean onLine,boolean toStem) throws Exception {
        String newQuery = "";
        String [] splitedQuery = query.split(" ");
        for(String word:splitedQuery){//all the words here are after stemming (if needed)
//            if(toStem){
//                String d= word;
//                stemmer.setCurrent(d); //set string you need to stem
//                stemmer.stem();  //stem the word
//                d= stemmer.getCurrent();//get the stemmed word
//                weights.put(d.toLowerCase(), (double)1);
//            }
        //    else{
                weights.put(word.toLowerCase(),(double)1);
           // }
            newQuery=newQuery+" "+word;
            if(toUseSemantic){
//                String temp=word;
//                if(toStem){
//                    String d= word;
//                    stemmer.setCurrent(d); //set string you need to stem
//                    stemmer.stem();  //stem the word
//                    temp= stemmer.getCurrent();//get the stemmed word
//                }
                if(onLine){
                    newQuery=newQuery+" "+ searchSynonym(word,toStem);
                }
                else{
                    newQuery=newQuery+" "+searchSynonimOffLine(word,toStem);
                }

            }
        }
//        System.out.println("i finished with semantic part");
//        System.out.println("my old query is:"+query);
//        System.out.println("my new query is:"+newQuery);
        return newQuery;
    }

    public String searchSynonimOffLine(String wordToSearch, boolean toStem){
        Word2VecModel Model = null;
        String listOfSynonym="";

        try {
            Model = Word2VecModel.fromTextFile(new File("word2vec.c.output.model.txt"));
        } catch (IOException e) {
            e.printStackTrace();
        }
        com.medallia.word2vec.Searcher semabticSearcher = Model.forSearch();
        int numOfResultsinList = 3;
        List<Searcher.Match> matches = null;
        try {
            matches = semabticSearcher.getMatches(wordToSearch,numOfResultsinList);
        } catch (Searcher.UnknownWordException e) {
            return listOfSynonym;
        }
     //   System.out.println("synonym words of:"+wordToSearch);
        for(com.medallia.word2vec.Searcher.Match match: matches){
            String s= match.match();

            if(!wordToSearch.equals(s)){
                if(toStem){
                    stemmer.setCurrent(s); //set string you need to stem
                    stemmer.stem();  //stem the word
                    s= stemmer.getCurrent();//get the stemmed word
                }
                listOfSynonym=listOfSynonym+s+" ";
                if(!s.contains(" ")){
                    weights.put(s,0.5);
                   // System.out.println("i added to weights the word:"+s);
                }
            }
        }





        return listOfSynonym;
    }
    public String searchSynonym(String wordToSearch, boolean toStem) throws Exception {
        String listOfSynonym="";
        String url = "https://api.datamuse.com/words?rel_syn=" + wordToSearch;

        URL obj = new URL(url);
        HttpURLConnection con = (HttpURLConnection) obj.openConnection();

        con.setRequestMethod("GET");
        con.setRequestProperty("User-Agent", USER_AGENT);

        // ordering the response
        StringBuilder response;
        try (BufferedReader in = new BufferedReader(new InputStreamReader(con.getInputStream()))) {
            String inputLine;
            response = new StringBuilder();

            while ((inputLine = in.readLine()) != null) {
                response.append(inputLine);
            }
        }

        ObjectMapper mapper = new ObjectMapper();

        try {
            // converting JSON array to ArrayList of words
            ArrayList<Word> words = mapper.readValue(
                    response.toString(),
                    mapper.getTypeFactory().constructCollectionType(ArrayList.class, Word.class)
            );

     //      System.out.println("Synonym word of '" + wordToSearch + "':");
            if(words.size() > 0) {
                for(Word word : words) {
                    if(word.getScore()>5000){
                        Double weight = 0.4;
                        if (toStem) {
                            String d= word.getWord();
                            stemmer.setCurrent(d); //set string you need to stem
                            stemmer.stem();  //stem the word
                             d= stemmer.getCurrent();//get the stemmed word
                            if(!d.contains(" ")){
                                weights.put(d,weight);
                                listOfSynonym=listOfSynonym+" "+d;
                            }
                        }
                        else{
                            if(!word.getWord().contains(" ")){
                                weights.put(word.getWord(), weight);
                                listOfSynonym=listOfSynonym+" "+word.getWord();
                            }
                        }

                    }
                    else if(word.getScore()>1000){
                        Double weight = 0.0;
                        if (toStem) {
                            String d= word.getWord();
                            stemmer.setCurrent(d); //set string you need to stem
                            stemmer.stem();  //stem the word
                            d= stemmer.getCurrent();//get the stemmed word
                            if(!d.contains(" ")){
                                weights.put(d,weight);
                                listOfSynonym=listOfSynonym+" "+d;
                               // System.out.println("stemmer - add word:"+d);
                            }
                        }
                        else{
                            if(!word.getWord().contains(" ")){
                                weights.put(word.getWord(), weight);
                                listOfSynonym=listOfSynonym+" "+word.getWord();
                            }
                        }

//                        weights.put(word.getWord(), weight);
//                        listOfSynonym=listOfSynonym+" "+word.getWord();
                    }

                   // System.out.println((words.indexOf(word) + 1) + ". " + word.getWord() + ", the score is:"+ word.getScore()+"");
                }
            }
            else {
               // System.out.println("none synonym word!");
            }
        }
        catch (IOException e) {
            e.getMessage();
        }
        return listOfSynonym;
    }

   // // word and score attributes are from DataMuse API
    static class Word {
        private String word;
        private int score;

        public String getWord() {
            return this.word;
        }

        public int getScore() {
            return this.score;
        }


    }
}
