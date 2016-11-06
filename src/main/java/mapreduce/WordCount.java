package mapreduce;

import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.fs.Path;
import org.apache.hadoop.io.IntWritable;
import org.apache.hadoop.io.Text;
import org.apache.hadoop.mapreduce.Job;
import org.apache.hadoop.mapreduce.Mapper;
import org.apache.hadoop.mapreduce.Reducer;
import org.apache.hadoop.mapreduce.lib.input.FileInputFormat;
import org.apache.hadoop.mapreduce.lib.output.FileOutputFormat;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;
import java.util.StringTokenizer;

public class WordCount {

    private static final Logger LOGGER = LoggerFactory.getLogger(WordCount.class);

    public static class TokenizerMapper extends Mapper<Object, Text, Text, IntWritable> {

        private final Text word = new Text();
        private final IntWritable one = new IntWritable(1);

        @Override
        public void map(Object key, Text value, Context context) throws IOException, InterruptedException {
            StringTokenizer itr = new StringTokenizer(value.toString());
            while (itr.hasMoreTokens()) {
                word.set(itr.nextToken());
                context.write(word, one);
            }
        }
    }

    public static class IntSumReducer extends Reducer<Text,IntWritable,Text,IntWritable> {

        private final IntWritable result = new IntWritable();

        @Override
        public void reduce(Text key, Iterable<IntWritable> values, Context context) throws IOException, InterruptedException {
            int sum = 0;
            for (IntWritable val : values) {
                sum += val.get();
            }
            result.set(sum);
            context.write(key, result);
        }
    }

    public static void main(String[] args) throws IOException, ClassNotFoundException, InterruptedException {

        Configuration conf = new Configuration();

        conf.set("yarn.resourcemanager.address", "localhost:8032");
        conf.set("fs.defaultFS", "hdfs://localhost:9000");
        conf.set("yarn.application.classpath", "/usr/local/hadoop/etc/hadoop,/usr/local/hadoop/share/hadoop/common/*,/usr/local/hadoop/share/hadoop/common/lib/*,/usr/local/hadoop/share/hadoop/hdfs/*,/usr/local/hadoop/share/hadoop/hdfs/lib/*,/usr/local/hadoop/share/hadoop/mapreduce/*,/usr/local/hadoop/share/hadoop/mapreduce/lib/*,/usr/local/hadoop/share/hadoop/yarn/*,/usr/local/hadoop/share/hadoop/yarn/lib/*");
        conf.set("mapreduce.framework.name", "yarn");
        conf.set("yarn.app.mapreduce.am.staging-dir", "/user");

        Job job = Job.getInstance(conf, "word count");
//        job.setJarByClass(WordCount.class);
        File jobJarFile = new File("./build/libs/mapreduce.jar");
        job.setJar(jobJarFile.toURI().toString());
        job.setMapperClass(TokenizerMapper.class);
        job.setCombinerClass(IntSumReducer.class);
        job.setReducerClass(IntSumReducer.class);
        job.setOutputKeyClass(Text.class);
        job.setOutputValueClass(IntWritable.class);

        FileInputFormat.addInputPath(job, new Path(args[0]));
        FileOutputFormat.setOutputPath(job, new Path(args[1]));
        System.exit(job.waitForCompletion(true) ? 0 : 1);

    }

//      Useful:

//        docker pull sequenceiq/hadoop-docker:2.7.0
//        docker run -p 9000:9000 -p 8032:8032 -it sequenceiq/hadoop-docker:2.7.0 /etc/bootstrap.sh -bash

//        bin/hdfs getconf -confKey KEY_NAME

//        cd $HADOOP_PREFIX
//        bin/hadoop fs -mkdir /user/massimo
//        bin/hadoop fs -chown massimo:massimo /user/massimo
}
