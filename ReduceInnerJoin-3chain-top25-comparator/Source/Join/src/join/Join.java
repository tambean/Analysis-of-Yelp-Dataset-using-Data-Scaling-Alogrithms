/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package join;

import java.io.IOException;
import java.util.ArrayList;
import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.fs.Path;
import org.apache.hadoop.io.IntWritable;
import org.apache.hadoop.io.LongWritable;
import org.apache.hadoop.io.Text;
import org.apache.hadoop.io.WritableComparable;
import org.apache.hadoop.io.WritableComparator;
import org.apache.hadoop.mapreduce.Job;
import org.apache.hadoop.mapreduce.Mapper;
import org.apache.hadoop.mapreduce.Reducer;
import org.apache.hadoop.mapreduce.lib.input.FileInputFormat;
import org.apache.hadoop.mapreduce.lib.input.MultipleInputs;
import org.apache.hadoop.mapreduce.lib.input.TextInputFormat;
import org.apache.hadoop.mapreduce.lib.output.FileOutputFormat;
import org.apache.hadoop.mapreduce.lib.output.TextOutputFormat;
/**
 *
 * @author anike
 */
public class Join {

    static int check = 0;

    public static class BusinessJoinMapper extends Mapper<Object, Text, Text, Text> {

        private Text outkey = new Text();
        private Text outvalue = new Text();

        public void map(Object key, Text value, Context context) throws IOException, InterruptedException {

            if (value.toString().contains("business_id")) {
                return;
            }
            String[] separatedInput = value.toString().split(",");
            if (separatedInput.length < 6) {
                return;
            }
            String businessId = separatedInput[0];
            if (businessId == null) {
                return;
            }
            outkey.set(businessId);
            outvalue.set("A" + separatedInput[0] + " " + separatedInput[2]);
            context.write(outkey, outvalue);
        }
    }

    public static class TipJoinMapper extends Mapper<Object, Text, Text, Text> {

        private Text outkey = new Text();
        private Text outvalue = new Text();

        public void map(Object key, Text value, Context context) throws IOException, InterruptedException {
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
            String businessId = separatedInput[1];
            if (businessId == null) {
                return;
            }
            outkey.set(businessId);
            outvalue.set("B" + separatedInput[4]);
            context.write(outkey, outvalue);
        }
    }

    public static class JoinReducer extends Reducer<Text, Text, Text, Text> {

        private static final Text EMPTY_TEXT = new Text("");
        private Text tmp = new Text();
        private ArrayList<Text> listA = new ArrayList<Text>();
        private ArrayList<Text> listB = new ArrayList<Text>();
        private String joinType = null;

        public void setup(Context context) {
            joinType = context.getConfiguration().get("join.type");
        }

        public void reduce(Text key, Iterable<Text> values, Context context) throws IOException, InterruptedException {
            // Clear our lists
            listA.clear();
            listB.clear();

            while (values.iterator().hasNext()) {
                tmp = values.iterator().next();
                if (Character.toString((char) tmp.charAt(0)).equals("A")) {

                    listA.add(new Text(tmp.toString().substring(1)));
                }
                if (Character.toString((char) tmp.charAt(0)).equals("B")) {
                    listB.add(new Text(tmp.toString().substring(1)));
                }
            }
            executeJoinLogic(context);
        }

        private void executeJoinLogic(Context context) throws IOException, InterruptedException {

            if (joinType.equalsIgnoreCase("inner")) {
                if (!listA.isEmpty() && !listB.isEmpty()) {
                    for (Text A : listA) {
                        for (Text B : listB) {
                            context.write(A, B);
                        }
                    }
                }
            }
        }
    }

    public static class MyMapper
            extends Mapper<LongWritable, Text, Text, IntWritable> {

        private final static IntWritable one = new IntWritable(1);
        String var = null;

        public void map(LongWritable key, Text value, Context context
        ) throws IOException, InterruptedException {
            var = value.toString();

            if (var.isEmpty()) {
                return;
            }

            String[] fields = var.split("\t");
            String tmp = new String(fields[0]);
            String[] token = tmp.split(" ");
            String place = token[1];
            context.write(new Text(place), one);

        }
    }

    public static class MyReducer
            extends Reducer<Text, IntWritable, Text, IntWritable> {

        public void reduce(Text key, Iterable<IntWritable> values,
                Context context
        ) throws IOException, InterruptedException {
            int sum = 0;

            for (IntWritable k : values) {
                sum += k.get();
            }
            context.write(key, new IntWritable(sum));
        }
    }

    public static class Mapper2 extends Mapper<LongWritable, Text, IntWritable, Text> {

        public void map(LongWritable fileOffset, Text lineContents,
                Context context)
                throws IOException, InterruptedException {

            String temp;
            temp = lineContents.toString();
            String[] lineData = lineContents.toString().split("\t");
            Text place = new Text(lineData[0]);
            IntWritable sum = new IntWritable(Integer.parseInt(lineData[1].trim()));
            context.write(sum, place);
        }
    }

    public static class Reducer2 extends Reducer<IntWritable, Text, Text, IntWritable> {

        public void reduce(IntWritable sum, Iterable<Text> id,
                Context context) throws IOException, InterruptedException {

            int count = 0;
            for (Text key : id) {
                check++;

                if (check <= 25) {

                    context.write(key, sum);
                } else {
                    //System.out.println("break");
                    break;
                }

            }
        }
    }

    public static class sortComparator extends WritableComparator {

        protected sortComparator() {
            super(IntWritable.class, true);

        }

        @Override
        public int compare(WritableComparable o1, WritableComparable o2) {
            IntWritable k1 = (IntWritable) o1;
            IntWritable k2 = (IntWritable) o2;
            int cmp = k1.compareTo(k2);
            return -1 * cmp;
        }

    }

    public static int main(String[] args) throws IOException, InterruptedException, ClassNotFoundException {
        Configuration conf = new Configuration();
        Job job = new Job(conf, "ReduceSideJoin");
        job.setJarByClass(Join.class);

        MultipleInputs.addInputPath(job, new Path(args[0]), TextInputFormat.class, BusinessJoinMapper.class);
        MultipleInputs.addInputPath(job, new Path(args[1]), TextInputFormat.class, TipJoinMapper.class);
        job.getConfiguration().set("join.type", "inner");
        job.setReducerClass(JoinReducer.class);

        job.setOutputFormatClass(TextOutputFormat.class);
        TextOutputFormat.setOutputPath(job, new Path(args[2]));

        job.setOutputKeyClass(Text.class);
        job.setOutputValueClass(Text.class);

        boolean success = job.waitForCompletion(true);
        if (success) {
            Job job2 = new Job(conf, "Job 2");
            job2.setJarByClass(Join.class);

            job2.setMapperClass(MyMapper.class);
            job2.setReducerClass(MyReducer.class);
            
            job2.setMapOutputKeyClass(Text.class);
            job2.setMapOutputValueClass(IntWritable.class);
            job2.setOutputKeyClass(Text.class);
            job2.setOutputValueClass(IntWritable.class);

            FileInputFormat.addInputPath(job2, new Path(args[2]));
            FileOutputFormat.setOutputPath(job2, new Path(args[3]));
            boolean flag = job2.waitForCompletion(true);
            if (flag) {

                Job job3 = Job.getInstance(conf, "Sorting");
                job3.setJarByClass(Join.class);
                job3.setMapperClass(Mapper2.class);
                job3.setReducerClass(Reducer2.class);
                job3.setSortComparatorClass(sortComparator.class);

                job3.setMapOutputKeyClass(IntWritable.class);
                job3.setMapOutputValueClass(Text.class);

                job3.setOutputKeyClass(Text.class);
                job3.setOutputValueClass(IntWritable.class);

                FileInputFormat.addInputPath(job3, new Path(args[3]));
                FileOutputFormat.setOutputPath(job3, new Path(args[4]));
                System.exit(job3.waitForCompletion(true) ? 0 : 1);

            }
        }
        return 0;
    }
}
