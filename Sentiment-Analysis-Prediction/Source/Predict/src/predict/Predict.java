/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package predict;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.text.DateFormatSymbols;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.StringTokenizer;
import java.util.logging.Level;
import java.util.logging.Logger;
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
public class Predict {

  
    public static class TokenizerMapper
            extends Mapper<Object, Text, Text, Text> {

        Set<String> positive = new HashSet<String>();
        Set<String> negative = new HashSet<String>();
        private final static IntWritable one = new IntWritable(1);
        String var = null;
        private final static SimpleDateFormat fmt = new SimpleDateFormat("MM/dd/yyyy");
        private BufferedReader positiveReader;
        private BufferedReader negativeReader;

        @Override
        protected void setup(Context context) throws IOException, InterruptedException {
             File positiveWordsFiles = new File("positivewords");
                FileInputStream positiveFiles = new FileInputStream(positiveWordsFiles);
                positiveReader = new BufferedReader(new InputStreamReader(positiveFiles));
                String stopWord = null;
                while ((stopWord = positiveReader.readLine()) != null) {
                    
                    positive.add(stopWord.toLowerCase()); 
                }
                File negativeWordsFiles = new File("negativewords");
                FileInputStream negativefiles = new FileInputStream(negativeWordsFiles);
                negativeReader = new BufferedReader(new InputStreamReader(negativefiles));
                String stop = null;
                while ((stop = negativeReader.readLine()) != null) {
                    negative.add(stop.toLowerCase()); 
                }

        }

        @Override
        protected void map(Object key, Text value, Context context) throws IOException, InterruptedException {
           
            if (value.toString().contains("business_id")) {
                return;
            }
            var = value.toString();
            String[] fields = var.split(",");
            if (fields.length < 6) {
                return;
            }
            String review_date = new String(fields[6]);
            Calendar c = Calendar.getInstance();
            Date dt;
            DateFormatSymbols dfs = new DateFormatSymbols();
            String[] months = dfs.getMonths();
            String mon = "";
            try {
                dt = fmt.parse(review_date);
                c.setTime(dt);
            } catch (ParseException ex) {
                Logger.getLogger(Predict.class.getName()).log(Level.SEVERE, null, ex);
            }
            
            int month = c.get(Calendar.MONTH);
            if (month >= 0 && month <= 11) {
                mon = months[month];
            }
            String rating = new String(fields[5]);
            String text = new String(fields[9]);
            int positiveCount = 0;
            int negativeCount = 0;
            StringTokenizer st = new StringTokenizer(text, " ");
            List<String> review = new ArrayList<String>();
            while (st.hasMoreElements()) {
                review.add(st.nextElement().toString());
            }
            for (String s : review) {
                if (positive.contains(s.toLowerCase())) {
                    positiveCount += 1;
                } else if (negative.contains(s.toLowerCase())) {
                    negativeCount += 1;
                }
            }
            double positiveRatio = (double)positiveCount / (double)review.size();
            double negativeRatio = (double)negativeCount / (double)review.size();
            double val = (positiveRatio - negativeRatio);            
            context.write(new Text(mon), new Text(rating + "," + Double.toString(val)));
        }
    }

    public static class IntSumReducer
            extends Reducer<Text, Text, Text, Text> {
        DoubleWritable reduce = new DoubleWritable();

        @Override
        protected void reduce(Text key, Iterable<Text> values, Context context) 
                throws IOException, InterruptedException {
            double sum = 0;
            int count = 0;
            double rate = 0;
            double avg = 0;
            double av = 0;
            for (Text val : values) {
                String []token = val.toString().split(",");
                sum += Double.parseDouble(token[0]);
                rate += Double.parseDouble(token[1]); 
                count++;
            }
            avg = rate/count;
            av = sum/count;
            context.write(key,new Text( Double.toString(avg)+"\t" + Double.toString(av)));
        }
    }
    
    public static void main(String[] args) throws IOException, InterruptedException, ClassNotFoundException {
        
        Configuration conf = new Configuration();

        String input = args[1];
        String output = args[2];
        Job job = new Job(conf, "count");
        job.setJarByClass(Predict.class);
        job.setMapperClass(TokenizerMapper.class);
        job.setReducerClass(IntSumReducer.class);
        job.setMapOutputKeyClass(Text.class);
        job.setMapOutputValueClass(Text.class);
        job.setOutputKeyClass(Text.class);
        job.setOutputValueClass(Text.class);
        FileInputFormat.addInputPath(job, new Path(args[1]));
        FileOutputFormat.setOutputPath(job, new Path(args[2]));
        job.addCacheFile(new Path(args[3]).toUri()); //distributed cache
        job.addCacheFile(new Path(args[4]).toUri()); //distributed cache
        System.exit(job.waitForCompletion(true) ? 0 : 1);
    }
    
}
