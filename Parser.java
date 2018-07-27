package ARC;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.stream.IntStream;

public class Parser {
    
    public ArrayList<String> myDocs;
    public ArrayList<Integer> myLabels;
    public ArrayList<String> termList;
    private static String[] stopList ;
    public static ArrayList<Integer> termFrequency;
    public ArrayList<ArrayList<Integer>> docLists;
    public Data data;

    public Parser(){
    	data = new Data();
        data.init();
        // data.showSomeData();
        String content= data.content;
        myDocs = data.getDocs();
        myLabels = data.getLabels();
        //System.out.println(content);
        
        termList = new ArrayList<String>();
        termFrequency = new ArrayList<Integer>();
        //load stopwords and sort
        //File test = new File("index.html");
        //System.out.println(test.listFiles());
        stopList = loadStopwords("src/ARC/input/stopwords.txt");
        System.out.println("Stopwords: " + stopList.length);
        Arrays.sort(stopList);
        
        ArrayList<String> terms = tokenization(content);
        //System.out.println("No.of terms: "+ Integer.toString(terms.size()));
        sortTerms(terms);
        
        // Get indices to split data set
        int[] indices = IntStream.rangeClosed(0, myDocs.size()-1).toArray();
        int splitIndex = (int)Math.floor(myDocs.size()*0.8);
        System.out.println("Splitting on " + splitIndex);
        
        // Split dataset into training and test sets
        ArrayList<String> trainingDocs = new ArrayList<String>(myDocs.subList(0, splitIndex));
        ArrayList<Integer> trainingLabels = new ArrayList<Integer>(myLabels.subList(0, splitIndex));
        ArrayList<String> testDocs = new ArrayList<String>(myDocs.subList(splitIndex, myDocs.size()));
        ArrayList<Integer> testLabels = new ArrayList<Integer>(myLabels.subList(splitIndex, myLabels.size()));
        
        // Naive Bayes
        Classifier nbc = new Classifier(trainingDocs, trainingLabels);
        double accuracy = nbc.classifyAll(testDocs, testLabels);
        System.out.println("Accuracy: " + accuracy);
    }

    public String[] loadStopwords(String stopwordsFile){

        String[] stopWords = null;
        String alllines= "";
        try{
            BufferedReader reader = new BufferedReader(new FileReader(stopwordsFile));
            String allLines = new String();
            String line = null;
            while((line=reader.readLine())!=null){
                allLines += line.toLowerCase()+"\n"; //case folding
            }
            stopWords = allLines.split("\n");
        }catch(IOException ioe){
            ioe.printStackTrace();
        }
        return stopWords;
    }


    /**
	 * Searches through stoplist to see if key is a stopword.
	 * 
	 * @param key A potential stopword
	 * @return index of match, or -1 if no match found
	 */
	public static int searchStopword(String key) {
		// Establish index range of stoplist segment, 
		// first starting with entire stoplist
		int low = 0; // first index 
		int high = stopList.length - 1; // last index 
		
		// Perform binary search
		while (low <= high) {
			// Get the middle stopword in the current segment of the stoplist
			int mid = low + (high - low)/2;
			// See if there is a match, using compareTo because, 
			// given we have sorted the stoplist alphabetically,
			// compareTo returns a negative integer, zero, or a positive integer 
			// if key is less than, equal to, or greater than stopword
			int result = key.compareTo(stopList[mid]);
			// If key is less than stopword, shorten right end of stoplist segment
			if (result < 0 ) {
				high = mid - 1; 
			} else if (result > 0) {
				// If key is greater than stopword, shorten left end of stoplist segment
				low = mid + 1;
			} else {
				// If key is equal to zero, key matches stopword
				// Return index of stopword
				return mid;
			}
		}
		
		// Return -1 if no match is found
		return -1;
    }

    public String stemming(String token){
        Stemmer st = new Stemmer();
        st.add(token.toCharArray(), token.length());
        st.stem();
        return st.toString();
    }

    public void sortTerms(ArrayList<String> terms) {
        ArrayList<Node> listNode = new ArrayList<>();

        for (String term : terms){
            int index = termList.indexOf(term);
            int tf = termFrequency.get(index);
            Node node = new Node(tf, term);
            listNode.add(node);
            Collections.sort(listNode, (a, b) -> a.freq - b.freq);
        }

        for(int k=0; k<listNode.size(); k++){
            String s= (listNode.get(k)).word;
            Integer tf = ((listNode.get(k)).freq);
            System.out.println(s+"\t"+ Integer.toString(tf));
        }

    }


    public ArrayList<String> tokenization(String review){

        /*
		   Tokenization: Creates an array of tokens
		 */
        String[] tokens = null;
        review  = review.toLowerCase();
        tokens  = review.split("[ .,&%$#!/+()-*^?:\"--]+");
        System.out.println("No.of tokens: "+ tokens.length);
        for (String token : tokens) {
            if (searchStopword(token) == -1 && token.length()!=1){
                token = stemming(token);
                //System.out.println("token: "+token);
                if (!termList.contains(token)) {//a new term
                    termList.add(token);
                    termFrequency.add(1);
                    /*
                    //docList = new ArrayList<Integer>();
                    //docList.add(i);
                    //docLists.add(docList);
                    */

                } else {//an existing term
                    int index = termList.indexOf(token);
                    int tf = termFrequency.get(index);
                    tf++;
                    termFrequency.set(index, tf);
                    }
                }
            }
        return termList;
        }


    public static void main(String[] args) {
    	Parser p = new Parser(); 
    }

}