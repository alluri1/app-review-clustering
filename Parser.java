package ARC;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

public class Parser {
    
	// Uncleaned documents/labels
    public ArrayList<String> myDocs;
    public ArrayList<Integer> myLabels;
    
    public ArrayList<String> termList;
    private static String[] stopList ;
    public static ArrayList<Integer> termFrequency;
    public ArrayList<ArrayList<Integer>> docLists;
    public Data data;
    ArrayList<String> vocabulary;
    ArrayList<Integer> termFreqs;
    
    ArrayList<String> cleanedTrainingDocs;
    ArrayList<String> cleanedTestDocs;
    ArrayList<Integer> cleanedTrainingLabels;
    ArrayList<Integer> cleanedTestLabels;
    ArrayList<Integer> trainingLabels;
    ArrayList<Integer> testLabels;

    // Values to use in user interface
    double avgAccuracy; // overall accuracy
    Classifier nbc2;
    int runs = 5;
    public String termStr;
    public int totalDocs;
    public int eliminatedDocs;
    public String dataExplorationStr;
    
    public String class0Reviews;
    public String class1Reviews;
    public String class2Reviews;
    public String class3Reviews;

    public Parser(){
    	data = new Data();
        data.init();
        // data.showSomeData();
        String content= data.content;
        myDocs = data.getDocs();
        myLabels = data.getLabels();
        data.showSomeData();
        dataExplorationStr = data.matrixString;
        //System.out.println(content);
        
        termList = new ArrayList<String>();
        termFrequency = new ArrayList<Integer>();
        //load stopwords and sort
        //File test = new File("index.html");
        //System.out.println(test.listFiles());
        stopList = loadStopwords("src/ARC/input/stopwords.txt");
        //System.out.println("Stopwords: " + stopList.length);
        Arrays.sort(stopList);
        
        ArrayList<String> terms = tokenization(content);
        //System.out.println("No.of terms: "+ Integer.toString(terms.size()));
        sortTerms(terms);
        
        sortReviewsIntoClasses();
    }
    
    /**
     * Sorts all loaded reviews into classes,
     * represented by strings that are easy to print.
     */
    public void sortReviewsIntoClasses() {
    	class0Reviews = new String();
    	class1Reviews = new String();
    	class2Reviews = new String();
    	class3Reviews = new String();
    	
    	for (int i = 0; i < myDocs.size(); i++) {
    		switch (myLabels.get(i)) {
    			case 0:
    				class0Reviews += myDocs.get(i) + "\n\n";
    				break;
    			case 1:
    				class1Reviews += myDocs.get(i) + "\n\n";
    				break;
    			case 2:
    				class2Reviews += myDocs.get(i) + "\n\n";
    				break;
    			case 3:
    				class3Reviews += myDocs.get(i) + "\n\n";
    				break;
    		}
    	}
    }
    
    public String classifyReview(String review) {
    	int label;
    	String reviewClass = new String();
    	
    	// Clean both training docs & review
        ArrayList<String> cleanedTrainingDocs = removeStopwords(myDocs, myLabels, "training");
        ArrayList<String> reviewList = new ArrayList<String>();
        reviewList.add(review);
        ArrayList<String> cleanedTestDocs = removeStopwords(reviewList, null, "test");
    	
    	Classifier nbc = new Classifier(cleanedTrainingDocs, cleanedTrainingLabels);
    	label = nbc.classify(cleanedTestDocs.get(0));
    	
    	System.out.println(reviewClass);
    	
    	switch (label) {
    		case 0:
    			reviewClass = "has_information_giving";
    			break;
   			case 1:
   				reviewClass = "has_information_seeking";
   				break;
   			case 2:
   				reviewClass = "has_feature_request";
   				break;
   			case 3:
   				reviewClass = "has_bug_report";
   				break;
    		
    	}
    	
        return reviewClass;
    }
    
    /** 
     * Keep in mind that this only evaluates the LAST iteration of the cross validation in trainTest...
     */
    public String evaluate() {
    	nbc2.evaluate();
    	
    	// Prepare a string to display on JTextArea
    	String matrixString = "Precision:\nMacroaveraged precision: " + nbc2.macroPrecision + "\n";
    	matrixString += "Microaveraged precision: " + nbc2.microPrecision + "\n";
        for (int j = 0; j < nbc2.numClasses; j++) {
        	String className = new String();
        	switch (j) {
        		case 0:
        			className = "has_information_giving";
        			break;
        		case 1:
        			className = "has_information_seeking";
        			break;
        		case 2:
        			className = "has_feature_request";
        			break;
        		case 3:
        			className = "has_bug_report";
        			break;
        	}
        	matrixString += "Precision of " + className + ": " + nbc2.precisions[j] + "\n";
        }
        matrixString += "\nRecall:\nMacroaveraged recall: " +  nbc2.macroRecall + "\n";
        matrixString += "Microaveraged recall: " + nbc2.microRecall + "\n";
        for (int j = 0; j < nbc2.numClasses; j++) {
        	String className = new String();
        	switch (j) {
    			case 0:
    				className = "has_information_giving";
    				break;
    			case 1:
    				className = "has_information_seeking";
    				break;
    			case 2:
    				className = "has_feature_request";
    				break;
    			case 3:
    				className = "has_bug_report";
    				break;
        	}
        	matrixString += "Recall of " + className + ": " + nbc2.recalls[j] + "\n";
        }
        
        matrixString += "\nAverage accuracy: " + avgAccuracy;
        return matrixString;
    }
    
    public void trainTest() {
    	// Initialize evaluation variables
        avgAccuracy = 0.0;
        double[] pooledInfoGivMatrix = new double[4]; // confusion matrix for class 0
        double[] pooledInfoReqMatrix = new double[4]; // confusion matrix for class 1
        double[] pooledFeatureReqMatrix = new double[4]; // confusion matrix for class 2
        double[] pooledBugReqMatrix = new double[4]; // confusion matrix for class 3
        
        // Get indices to split data set
        for (int i = 0; i < runs; i++) {
        	ArrayList<Integer> indices = new ArrayList<Integer>(); 
        	for (int j = 0; j < myDocs.size(); j++) {
        		indices.add(j);
        	}
        	
        	Collections.shuffle(indices);
        	System.out.println("First index is... " + indices.get(0));
        	
            int splitIndex = (int)Math.floor(myDocs.size()*0.8);
            
            ArrayList<Integer> trainIndices = new ArrayList<Integer>(indices.subList(0, splitIndex));
            ArrayList<Integer> testIndices = new ArrayList<Integer>(indices.subList(splitIndex,  myLabels.size()));
            
            // Split dataset into training and test sets 
            ArrayList<String> trainingDocs = (ArrayList<String>) trainIndices.stream()
                    .map(myDocs::get)
                    .collect(Collectors.toList());
            ArrayList<Integer> trainingLabels = (ArrayList<Integer>) trainIndices.stream()
                    .map(myLabels::get)
                    .collect(Collectors.toList());
            ArrayList<String> testDocs = (ArrayList<String>) testIndices.stream()
                    .map(myDocs::get)
                    .collect(Collectors.toList());
            ArrayList<Integer> testLabels = (ArrayList<Integer>) testIndices.stream()
                    .map(myLabels::get)
                    .collect(Collectors.toList());
            
            // Initialize tables to be filled for each run, then added to pooled tables
            double accuracy;
            double[] infoGivMatrix = new double[4]; // confusion matrix for class 0
            double[] infoReqMatrix = new double[4]; // confusion matrix for class 1
            double[] featureReqMatrix = new double[4]; // confusion matrix for class 2
            double[] bugReqMatrix = new double[4]; // confusion matrix for class 3
            
            // Naive Bayes without stopword removal, perform once 
            if (i == 0) {
            	System.out.println("\nWithout stopword removal:");
                Classifier nbc = new Classifier(trainingDocs, trainingLabels);
                accuracy = nbc.classifyAll(testDocs, testLabels);
                System.out.println(String.format("Accuracy: %2.3f", accuracy));
            }
            
            // Naive Bayes with stopword removal
            System.out.println();
            vocabulary = new ArrayList<String>();
            termFreqs = new ArrayList<Integer>();
            // Remove stopwords from training and test docs
            ArrayList<String> cleanedTrainingDocs = removeStopwords(trainingDocs, trainingLabels, "training");
            ArrayList<String> cleanedTestDocs = removeStopwords(testDocs, testLabels, "test");
            System.out.println("Training Docs: " + cleanedTrainingDocs.size() + " Labels: " + cleanedTrainingLabels.size());
            // Classify test docs
            nbc2 = new Classifier(cleanedTrainingDocs, cleanedTrainingLabels);
            System.out.println("Test Docs: " + cleanedTestDocs.size() + " Labels: " + cleanedTestLabels.size());
            System.out.println("No.of tokens: " + vocabulary.size());
            accuracy = nbc2.classifyAll(cleanedTestDocs, cleanedTestLabels);
            System.out.println(String.format("Accuracy: %2.3f", accuracy));
            avgAccuracy += accuracy;
        }
        avgAccuracy = (double)avgAccuracy/runs;
    }
    
    public ArrayList<String> removeStopwords(ArrayList<String> docs, ArrayList<Integer> labels, String which) {
    	ArrayList<String> cleanedDocs = new ArrayList<String>();
    	ArrayList<Integer> cleanedLabels = new ArrayList<Integer>();
    	
    	for (int i = 0; i < docs.size(); i++) {
        	ArrayList<String> review = new ArrayList<String>();
        	String[] tokens = docs.get(i).split("[ .,&%$#!/+()-*^?:\"--]+");
        	for (String token : tokens) {
        		// If new term & not a stopword && does not contain number
        		if (!vocabulary.contains(token) && searchStopword(token) == -1 && token.matches("\\b[(a-z)|(A-Z)]*\\b")) {        			
        			review.add(token); // keep in review
        			vocabulary.add(token);
        			termFreqs.add(1);
        		} else if (vocabulary.contains(token)) {
        			// If term exists in vocabulary (and is not stopword), increment freq
        			int index = vocabulary.indexOf(token);
        			int freq = termFreqs.get(index);
        			termFreqs.set(index, freq++);
        			review.add(token);
        		}
        	}
        	// Don't add review/label if it is empty
        	if (review.size() != 0) {
        		cleanedDocs.add(String.join(" ", review));
        		if (labels != null) {
        			cleanedLabels.add(labels.get(i));
        		}
        		
        	}
        }
    	
    	if (labels != null) {
    		if (which.equals("training")) {
        		cleanedTrainingLabels = cleanedLabels;
    		} else if (which.equals("test")) {
    			cleanedTestLabels = cleanedLabels;
    		}
    	}
    	
    	return cleanedDocs;
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
    	termStr = new String();
    	
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
            termStr += s + "\t" + Integer.toString(tf) + "\n";
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

        		// If new token, not a stopword, and does not contain number,
                if (!termList.contains(token) && searchStopword(token) == -1 && token.matches("\\b[(a-z)|(A-Z)]*\\b")) {
                    termList.add(token);
                    termFrequency.add(1);
                    /*
                    //docList = new ArrayList<Integer>();
                    //docList.add(i);
                    //docLists.add(docList);
                    */

                } else if (termList.contains(token)){//an existing term
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
