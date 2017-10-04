/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package userratedcount;

import java.io.IOException;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.fs.Path;
import org.apache.hadoop.io.DoubleWritable;
import org.apache.hadoop.io.IntWritable;
import org.apache.hadoop.io.Text;
import org.apache.hadoop.mapreduce.Job;
import org.apache.hadoop.mapreduce.Mapper;
import org.apache.hadoop.mapreduce.Reducer;
import org.apache.hadoop.mapreduce.lib.input.FileInputFormat;
import org.apache.hadoop.mapreduce.lib.output.FileOutputFormat;

/**
 *
 * @author anike
 */
public class UserRatedCount {

    public static class TokenizerMapper
            extends Mapper<Object, Text, Text, Text> {

        String temp = null;
        private final static IntWritable one = new IntWritable(1);
        private final Text mapKey = new Text();
        public void map(Object key, Text value, Context context
        ) throws IOException, InterruptedException {

            temp = value.toString();
            if (temp.isEmpty()) {
                return;
            }
            String[] fields = temp.split("\t");
            String movie_id = new String(fields[1]);
            String[] tokens = movie_id.toString().split(" ");
            if (tokens.length >= 2) {
                for (int i = 0; i < tokens.length; i++) {
                    for (int j = i + 1; j < tokens.length; j++) {
                        String groupings = tokens[i] + "," + tokens[j]; // Add 2 movies with ",". For e.g. "19,2,21,1"
                        String[] moviesAndRatings = groupings.split(",");
                        if (moviesAndRatings.length == 4) {
                            context.write(new Text(moviesAndRatings[0] + "," + moviesAndRatings[2]),
                                    new Text(moviesAndRatings[1] + "," + moviesAndRatings[3]));
                        }
                    }
                }
            }
        }
    }

    public static class IntSumReducer
            extends Reducer<Text, Text, Text, Text> {

        private Map<Text, DoubleWritable> topCountMap = new HashMap<Text, DoubleWritable>();
        public void reduce(Text key, Iterable<Text> values,
                Context context
        ) throws IOException, InterruptedException {

             double sum_aa = 0;
           double sum_bb =0;
           double sum_ab = 0;
           int count = 0;
           double score = 0;
           double num =0;
           double den = 0;
            for (Text val : values) {
                String [] token = val.toString().split(",");
                 double aa = Double.parseDouble(token[0]);
                 double bb = Double.parseDouble(token[1]);
                 sum_ab += aa * bb;
                 sum_aa += aa * aa;
                 sum_bb += bb * bb;
                count ++;
            }
            num = sum_ab;
            den = Math.sqrt(sum_aa) *Math.sqrt(sum_bb);
            if(den != 0){
                score = num/den;
            }
            //String p = "The number of movie rated by"+ key.toString();
            if(count >9){
             topCountMap.put(new Text(key +"\t"+ Integer.toString(count)), new DoubleWritable(score));
             //context.write(key, new Text(Double.toString(score) + "\t" + Integer.toString(count)));
            }
           // context.write(key, new Text(Integer.toString(count)));
        }
        
        @Override
        protected void cleanup(Context context) throws IOException, InterruptedException {
            
            Map<Text, DoubleWritable> sortedValues = sortByValues(topCountMap);
         
            int count = 0;
            for(Text key: sortedValues.keySet()){
                if(count++ == 25){
                    
                    break;
                }
                context.write(key, new Text(sortedValues.get(key).toString()));
            }
            
        }
    
    private static Map<Text, DoubleWritable> sortByValues(Map<Text, DoubleWritable> map) {
        List<Map.Entry<Text, DoubleWritable>> entries = new LinkedList<Map.Entry<Text, DoubleWritable>>(map.entrySet());

        Collections.sort(entries, new Comparator<Map.Entry<Text, DoubleWritable>>() {

           
            public int compare(Map.Entry<Text, DoubleWritable> o1, Map.Entry<Text, DoubleWritable> o2) {
                return o2.getValue().compareTo(o1.getValue());
            }
        });

        Map<Text, DoubleWritable> sortedMap = new LinkedHashMap<Text, DoubleWritable>();

        for (Map.Entry<Text, DoubleWritable> entry : entries) {
            sortedMap.put(entry.getKey(), entry.getValue());
        }

        return sortedMap;
    }
        
        
    }

    /**
     * @param args the command line arguments
     */
    public static void main(String[] args) throws IOException, InterruptedException, ClassNotFoundException {
        Configuration conf = new Configuration();

        String input = args[1];
        String output = args[2];
        Job job = new Job(conf, "User Count");
        job.setJarByClass(UserRatedCount.class);
        job.setMapperClass(TokenizerMapper.class);
        job.setReducerClass(IntSumReducer.class);
        job.setMapOutputKeyClass(Text.class);
        job.setMapOutputValueClass(Text.class);
        job.setOutputKeyClass(Text.class);
        job.setOutputValueClass(Text.class);
        FileInputFormat.addInputPath(job, new Path(args[1]));
        FileOutputFormat.setOutputPath(job, new Path(args[2]));
        System.exit(job.waitForCompletion(true) ? 0 : 1);
    }

}
