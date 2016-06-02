import java.io.IOException;
import java.util.Iterator;
import java.util.StringTokenizer;

import org.apache.hadoop.conf.Configured;
import org.apache.hadoop.fs.FileSystem;
import org.apache.hadoop.fs.Path;
import org.apache.hadoop.io.IntWritable;
import org.apache.hadoop.io.LongWritable;
import org.apache.hadoop.io.Text;
import org.apache.hadoop.mapreduce.Job;
import org.apache.hadoop.mapreduce.Mapper;
import org.apache.hadoop.mapreduce.Reducer;
import org.apache.hadoop.mapreduce.lib.input.FileInputFormat;
import org.apache.hadoop.mapreduce.lib.output.FileOutputFormat;
import org.apache.hadoop.mapreduce.lib.output.SequenceFileOutputFormat;
import org.apache.hadoop.util.Tool;
import org.apache.hadoop.util.ToolRunner;
import org.apache.log4j.Logger;

public class BigramCount extends Configured implements Tool {
  private static final Logger LOG = Logger.getLogger(BigramCount.class);

  protected static class MyMapper extends Mapper<LongWritable, Text, Text, IntWritable> {
    private static final IntWritable one = new IntWritable(1);
    private static final Text bg = new Text();

    @Override
    public void map(LongWritable key, Text value, Context context)
        throws IOException, InterruptedException {
      String line = value.toString();

      String previous = null;
      StringTokenizer itr = new StringTokenizer(line);
      while (itr.hasMoreTokens()) {
        String cur = itr.nextToken();

        if (previous != null) {
          bg.set(previous + " " + cur);
          context.write(bg, one);
        }
        previous = cur;
      }
    }
  }

  protected static class MyReducer extends Reducer<Text, IntWritable, Text, IntWritable> {
    private final static IntWritable SUM = new IntWritable();

    @Override
    public void reduce(Text key, Iterable<IntWritable> values, Context context) throws IOException,
        InterruptedException {
      int sum = 0;
      Iterator<IntWritable> iter = values.iterator();
      while (iter.hasNext()) {
        sum += iter.next().get();
      }
      SUM.set(sum);
      context.write(key, SUM);
    }
  }

  private BigramCount() {}

  private static int printUsage(String[] args) {
    System.out.println("BigramCount : inputPath outputPath #reducers");
    for (int i = 0; i< args.length; i++) {
        String param = args[i];
        System.out.println(param);
    }
    ToolRunner.printGenericCommandUsage(System.out);
    return -1;
  }

  public int run(String[] args) throws Exception {
    if (args.length != 3) {
      printUsage(args);
      return -1;
    }

    String inputPath = args[0];
    String outputPath = args[1];
    int reduceTasks = Integer.parseInt(args[2]);

    Job job = Job.getInstance(getConf());
    job.setJobName(BigramCount.class.getSimpleName());
    job.setJarByClass(BigramCount.class);

    job.setNumReduceTasks(reduceTasks);

    FileInputFormat.setInputPaths(job, new Path(inputPath));
    FileOutputFormat.setOutputPath(job, new Path(outputPath));

    job.setMapOutputKeyClass(Text.class);
    job.setMapOutputValueClass(IntWritable.class);
    job.setOutputKeyClass(Text.class);
    job.setOutputValueClass(IntWritable.class);
    job.setOutputFormatClass(SequenceFileOutputFormat.class);

    job.setMapperClass(MyMapper.class);
    job.setCombinerClass(MyReducer.class);
    job.setReducerClass(MyReducer.class);

    Path outputDir = new Path(outputPath);
    FileSystem.get(getConf()).delete(outputDir, true);

    long startTime = System.currentTimeMillis();
    job.waitForCompletion(true);
    System.out.println("Job Finished in " + (System.currentTimeMillis() - startTime) / 1000.0
        + " seconds");

    return 0;
  }

  /**
   * Dispatches command-line arguments to the tool via the {@code ToolRunner}.
   */
  public static void main(String[] args) throws Exception {
    ToolRunner.run(new BigramCount(), args);
  }
}
