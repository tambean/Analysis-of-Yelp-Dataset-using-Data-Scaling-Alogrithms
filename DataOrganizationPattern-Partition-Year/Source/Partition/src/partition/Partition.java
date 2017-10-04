/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package partition;

import java.io.IOException;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.apache.hadoop.conf.Configurable;
import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.fs.Path;
import org.apache.hadoop.io.IntWritable;
import org.apache.hadoop.io.NullWritable;
import org.apache.hadoop.io.Text;
import org.apache.hadoop.mapreduce.Job;
import org.apache.hadoop.mapreduce.Mapper;
import org.apache.hadoop.mapreduce.Partitioner;
import org.apache.hadoop.mapreduce.Reducer;
import org.apache.hadoop.mapreduce.lib.input.FileInputFormat;
import org.apache.hadoop.mapreduce.lib.input.TextInputFormat;
import org.apache.hadoop.mapreduce.lib.output.FileOutputFormat;

/**
 *
 * @author anike
 */
public class Partition {
    
    public static class PartitionMapper
            extends Mapper<Object, Text, IntWritable, Text> {
        
        String temp = null;
        private final static SimpleDateFormat fmt = new SimpleDateFormat("MM/dd/yyyy");
        private int year = 0;

        public void map(Object key, Text value, Mapper.Context context
        ) throws IOException, InterruptedException {
            
            if (value.toString().contains("business_id")) {
                return;
            }
            if (value.toString().isEmpty()) {
                return;
            }
            String[] separatedInput = value.toString().split(",");
            if (separatedInput.length < 6) {
                return;
            }
            String date = new String(separatedInput[2]);
            try {
                Calendar c = Calendar.getInstance();
                Date dt = fmt.parse(date);
                c.setTime(dt);
                year = c.get(Calendar.YEAR);
            } catch (ParseException ex) {
                Logger.getLogger(Partition.class.getName()).log(Level.SEVERE, null, ex);
            }

            // System.out.println("map*****************************");
            context.write(new IntWritable(year), value);
        }
    }
    
    public static class YearPartitoner extends Partitioner<IntWritable, Text>
            implements Configurable {
        
        private Configuration conf = null;
        private int year = 0;
        private static final String DATE_YEAR = "date.year";
        
        @Override
        public int getPartition(IntWritable key, Text value, int i) {
            return key.get() - year;
            
        }
        
        @Override
        public void setConf(Configuration conf) {
            this.conf = conf;
            year = conf.getInt(DATE_YEAR, 0);
        }
        
        @Override
        public Configuration getConf() {
            return conf;
        }
        
        public static void setAccessYear(Job job, int accessYear) {
            job.getConfiguration().setInt(DATE_YEAR, accessYear);
        }
        
    }
    
    
    public static class SumReducer
            extends Reducer<IntWritable, Text, Text, NullWritable> {

       
        public void reduce(IntWritable key, Iterable<Text> values,
               Reducer.Context context
        ) throws IOException, InterruptedException {
            
            for(Text t :values){
                context.write(t, NullWritable.get());
            }
        }
    }

    public static void main(String[] args) throws IOException, InterruptedException, ClassNotFoundException {
        
        Configuration conf = new Configuration();
        
        Job job = new Job(conf, "Partition");
        job.setJarByClass(Partition.class);
        
        job.setMapperClass(PartitionMapper.class);
        job.setInputFormatClass(TextInputFormat.class);
        job.setPartitionerClass(YearPartitoner.class);
        YearPartitoner.setAccessYear(job, 2009);
        job.setNumReduceTasks(8);
        job.setMapOutputKeyClass(IntWritable.class);
        job.setMapOutputValueClass(Text.class);
        job.setOutputKeyClass(Text.class);
        job.setOutputValueClass(NullWritable.class);
        FileInputFormat.addInputPath(job, new Path(args[1]));
        FileOutputFormat.setOutputPath(job, new Path(args[2]));
        System.exit(job.waitForCompletion(true) ? 0 : 1);
    }
    
}
