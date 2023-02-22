/*
ENTER YOUR NAME HERE
NAME: Theodore Pinto
MATRICULATION NUMBER: A0199277H
*/

import java.io.IOException;
import java.util.*;

import java.io.File;
import java.io.FileNotFoundException;


import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.fs.Path;
import org.apache.hadoop.io.IntWritable;
import org.apache.hadoop.io.Text;
import org.apache.hadoop.mapreduce.Job;
import org.apache.hadoop.mapreduce.Mapper;
import org.apache.hadoop.mapreduce.Reducer;
import org.apache.hadoop.mapreduce.lib.input.FileInputFormat;
import org.apache.hadoop.mapreduce.lib.output.FileOutputFormat;

import static java.lang.Integer.parseInt;

public class TopkCommonWords {

    public static HashSet <String> stopwords = new HashSet<String>();
    public static int kIterations;

    public static void main(String[] args) throws Exception{
        Configuration conf = new Configuration();
        Job job = Job.getInstance(conf, "TopkCommonWords");
        job.setJarByClass(TopkCommonWords.class);

        job.setMapperClass(TokenizerMapper.class);
        job.setCombinerClass(IntSumReducer.class);
        job.setReducerClass(IntSumReducer.class);

        job.setOutputKeyClass(Text.class);
        job.setOutputValueClass(IntWritable.class);

        // Read in all stopwords from stopword file (args[2]) and k times to run (args[4])
        scanStopwords(args[2]);
        kIterations = parseInt(args[4]);

        // Inputs from args[0] and args[1] (file 1 & 2), Output from arg[3] (output file)
        FileInputFormat.addInputPath(job, new Path(args[0]));
        FileInputFormat.addInputPath(job, new Path(args[1]));
        FileOutputFormat.setOutputPath(job, new Path(args[3]));
        System.exit(job.waitForCompletion(true) ? 0 : 1);
    }

    public static void scanStopwords(String stopwordFileName) throws FileNotFoundException {

        File stopwordFile = new File(stopwordFileName);
        Scanner fileScanner = new Scanner(stopwordFile);

        while(fileScanner.hasNextLine()){
            String stopword = fileScanner.nextLine();
            if(stopwords.contains(stopword)) {
                stopwords.add(stopword);
            }
        }
    }

    public static class TokenizerMapper extends Mapper<Object, Text, Text, IntWritable> {

        private HashMap<String, Integer> wordCountTracker = new HashMap<String, Integer>();

        public void map(Object key, Text value, Context context)
                throws IOException, InterruptedException {

            StringTokenizer itr = new StringTokenizer(value.toString());

            while (itr.hasMoreTokens()) {

                String currToken = itr.nextToken();

                // Do not consider words with length less than/equal to 4 or in stopwords list
                if (currToken.length() <= 4 || stopwords.contains(currToken)) {
                    continue;
                }

                // Increment present count if token exists, else add to hashmap with init value of 1
                if (wordCountTracker.containsKey(currToken)) {
                    wordCountTracker.put(currToken, wordCountTracker.get(currToken) + 1);
                } else {
                    wordCountTracker.put(currToken, 1);
                }

                // Iterate through all entries in hashmap, emitting <word, count> per pair
                for (Map.Entry<String, Integer> wordCountPair : wordCountTracker.entrySet()) {
                    String wordKey = wordCountPair.getKey();
                    int wordCount = wordCountPair.getValue();

                    Text word = new Text(wordKey);
                    IntWritable count = new IntWritable(wordCount);

                    context.write(word, count);
                }

            }
        }
    }

    public static class IntSumReducer extends Reducer<Text,IntWritable,Text,IntWritable> {

        public void reduce(Text key, Iterable<IntWritable> values, Context context)
                throws IOException, InterruptedException {

            int minCount = Integer.MAX_VALUE;
	        int fileCount = 0;
            
            for (IntWritable val : values) {
                if (minCount > val.get()) {
                    minCount = val.get();
                }
		        fileCount++;
            }

            if (fileCount != 2) {
                return;
            }

            
            IntWritable count = new IntWritable(minCount);

            context.write(key, count);
        }

        public void cleanup(Context context)
                throws IOException, InterruptedException {

        }
    }
}
