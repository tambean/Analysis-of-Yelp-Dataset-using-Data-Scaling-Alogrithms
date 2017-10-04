/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package reco;

import java.io.IOException;

import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.fs.Path;
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
public class Reco {

    public static class MedianMapper
            extends Mapper<Object, Text, Text, Tuple> {

        String temp = null;
        Tuple check = new Tuple();

        public void map(Object key, Text value, Mapper.Context context
        ) throws IOException, InterruptedException {

            if (value.toString().contains("business_id")) {
                return;
            }
            String[] separatedInput = value.toString().split(",");
            if (separatedInput.length < 9) {
                return;
            }
            String businessId = separatedInput[3];
            if (businessId == null) {
                return;
            }
            String user_id = new String(separatedInput[0]);
            String business_rating = new String(separatedInput[5]);
            double rating = Double.parseDouble(business_rating.trim());
            check.setRating(rating);            
            check.setBusinessid(businessId);
            context.write(new Text(user_id), check);
        }
    }

    public static class MedianReducer
            extends Reducer<Text, Tuple, Text, Text> {

        public void reduce(Text key, Iterable<Tuple> values,
                Context context
        ) throws IOException, InterruptedException {

            long count = 0;
            String t = "";

            for (Tuple val : values) {
                t = t.concat(val.getBusinessid()+ "," + val.getRating() + " ");
            }
            context.write(key, new Text(t.trim()));
        }

    }

    public static void main(String[] args) throws IOException, InterruptedException, ClassNotFoundException {

        Configuration conf = new Configuration();

        String input = args[1];
        String output = args[2];
        Job job = new Job(conf, "Reco");
        job.setJarByClass(Reco.class);

        job.setMapperClass(MedianMapper.class);
        job.setReducerClass(MedianReducer.class);
        job.setMapOutputKeyClass(Text.class);
        job.setMapOutputValueClass(Tuple.class);
        job.setOutputKeyClass(Text.class);
        job.setOutputValueClass(Text.class);
        FileInputFormat.addInputPath(job, new Path(args[1]));
        FileOutputFormat.setOutputPath(job, new Path(args[2]));
        System.exit(job.waitForCompletion(true) ? 0 : 1);
    }

}
