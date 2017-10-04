/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package sum;

import java.io.IOException;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.fs.Path;
import org.apache.hadoop.io.Text;
import org.apache.hadoop.mapreduce.Job;
import org.apache.hadoop.mapreduce.Mapper;
import org.apache.hadoop.mapreduce.Reducer;
import org.apache.hadoop.mapreduce.lib.input.FileInputFormat;
import org.apache.hadoop.mapreduce.lib.input.TextInputFormat;
import org.apache.hadoop.mapreduce.lib.output.FileOutputFormat;

/**
 *
 * @author anike
 */
public class Sum {

    public static class SumMapper
            extends Mapper<Object, Text, Text, Tuple> {

        String temp = null;
        private final static SimpleDateFormat fmt = new SimpleDateFormat("MM/dd/yyyy");
        Tuple check = new Tuple();

        public void map(Object key, Text value, Context context
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
            String userId = separatedInput[0];
            if (userId == null) {
                return;
            }
            String date = new String(separatedInput[2]);
            try {
                Date dt = fmt.parse(date);
                check.setFirst(dt);
                check.setLast(dt);
            } catch (ParseException ex) {
                Logger.getLogger(Sum.class.getName()).log(Level.SEVERE, null, ex);
            }
            check.setCount(1);
            // System.out.println("map*****************************");
            context.write(new Text(userId), check);
        }
    }

    public static class SumReducer
            extends Reducer<Text, Tuple, Text, Tuple> {

        private Tuple result = new Tuple();
       
        public void reduce(Text key, Iterable<Tuple> values,
               Context context
        ) throws IOException, InterruptedException {

            result.setFirst(null);
            result.setLast(null);
            result.setCount(0);
            int sum = 0;

            for (Tuple m : values) {
                
                if(result.getFirst() == null || 
                        m.getFirst().compareTo(result.getFirst()) <0){
                    result.setFirst(m.getFirst());
                }
                 if(result.getLast() == null || 
                        m.getLast().compareTo(result.getLast()) > 0){
                    result.setLast(m.getLast());
                }
                //System.out.println("reduce----------------------------");
                sum += m.getCount();
            }
            result.setCount(sum);
            context.write(key, result);
        }
    }

    public static void main(String[] args) throws IOException, InterruptedException, ClassNotFoundException {
        
        Configuration conf = new Configuration();
  
        Job job = new Job(conf, "Sum");
        job.setJarByClass(Sum.class);
        
        job.setMapperClass(SumMapper.class);
        job.setInputFormatClass(TextInputFormat.class);
        job.setReducerClass(SumReducer.class);
        job.setMapOutputKeyClass(Text.class);
        job.setMapOutputValueClass(Tuple.class);
        job.setOutputKeyClass(Text.class);
        job.setOutputValueClass(Tuple.class);
        FileInputFormat.addInputPath(job, new Path(args[1]));
        FileOutputFormat.setOutputPath(job, new Path(args[2]));
        System.exit(job.waitForCompletion(true) ? 0 : 1);
    }

}
