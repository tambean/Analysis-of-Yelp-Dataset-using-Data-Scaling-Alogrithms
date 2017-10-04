/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package filter;

import java.io.IOException;
import java.util.Random;

import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.fs.Path;

import org.apache.hadoop.io.NullWritable;
import org.apache.hadoop.io.Text;
import org.apache.hadoop.mapreduce.Job;
import org.apache.hadoop.mapreduce.Mapper;
import org.apache.hadoop.mapreduce.lib.input.FileInputFormat;
import org.apache.hadoop.mapreduce.lib.output.FileOutputFormat;

/**
 *
 * @author anike
 */
public class Filter {

    public static class TokenizerMapper
            extends Mapper<Object, Text, NullWritable, Text> {

         
        private Random randomPercent = new Random();
        private float percentage;

        @Override
        protected void setup(Context context) throws IOException, InterruptedException {
            percentage = .80f;
        }
        
        public void map(Object key, Text value, Mapper.Context context
        ) throws IOException, InterruptedException {
           
           if(randomPercent.nextDouble() < percentage){
               context.write(NullWritable.get(),value);
           }
        }
    }
    
    public static void main(String[] args) throws IOException, InterruptedException, ClassNotFoundException {
        // TODO code application logic here
         Configuration conf = new Configuration();

        Job job = new Job(conf, "filter");
        job.setJarByClass(Filter.class);
        job.setMapperClass(TokenizerMapper.class);
        job.setOutputKeyClass(NullWritable.class);
        job.setOutputValueClass(Text.class);
        job.setNumReduceTasks(1);  
             
        FileInputFormat.addInputPath(job, new Path(args[1]));
        FileOutputFormat.setOutputPath(job, new Path(args[2]));
        System.exit(job.waitForCompletion(true) ? 0 : 1);
    }
}
