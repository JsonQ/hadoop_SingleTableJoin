package hadoop_STjoin;

import java.io.IOException;
import java.util.StringTokenizer;

import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.fs.Path;
import org.apache.hadoop.io.Text;
import org.apache.hadoop.mapreduce.Job;
import org.apache.hadoop.mapreduce.Mapper;
import org.apache.hadoop.mapreduce.Reducer;
import org.apache.hadoop.mapreduce.lib.input.FileInputFormat;
import org.apache.hadoop.mapreduce.lib.output.FileOutputFormat;
import org.apache.hadoop.util.GenericOptionsParser;

public class SingleTableJoin {

	public static int times = 0;
	
	/*

     * map������ָ�child��parent��Ȼ���������һ����Ϊ�ұ�

     * �������һ����Ϊ�����Ҫע������������value�б���

     * �������ұ�������ʶ��

     */
	public static class Map extends Mapper<Object, Text, Text, Text>
	{
		protected void map(Object key, Text value, Context context)
				throws IOException, InterruptedException {
			String childName = new String(); //��������
			String parentName = new String(); //��ĸ����
			String relationType = new String(); //���ұ��־
			
			//����ÿһ��
			StringTokenizer iter = new StringTokenizer(value.toString());
			String[] values = new String[2];
			int i=0;
			while(iter.hasMoreTokens())
			{
				values[i] = iter.nextToken();
				i++;
			}
			
			if (i == 2 && values[0].compareTo("child") != 0) {
				childName = values[0];
				parentName = values[1];
				
				//������
				relationType = "1";
				context.write(new Text(parentName),new Text(relationType+"+"+childName+"+"+parentName));
				
				//����ұ�
				relationType = "2";
				context.write(new Text(childName), new Text(relationType+"+"+childName+"+"+parentName));

			}
			
		}
	}
	
	//reduce
	public static class Reduce extends Reducer<Text, Text, Text, Text>
	{
		
		protected void reduce(Text key, Iterable<Text> values, Context cont)
				throws IOException, InterruptedException {
			
			if (times == 0) {
				cont.write(new Text("grandChild"), new Text("grandParen"));
				times++;
			}
			
			int grandChildNum = 0;
			String[] grandChild = new String[10];
			int grandParentNum = 0;
			String[] grandParent = new String[10];
			
			System.out.println("key="+key.toString());
			
			for(Text temp : values)
			{
				String record = temp.toString();
				System.out.println("record="+record);
				if (record.length() == 0) {
					System.out.println("continue;");
					continue;
				}
				
				String childName = new String();
				String parenName = new String();
				
				int i = 2;
				while (record.charAt(i) != '+')
				{
					childName += record.charAt(i);
					i++;
				}
				i++;
				
				while(i < record.length())
				{
					parenName += record.charAt(i);
					i++;
				}
				
				char relationType = record.charAt(0);
				
				//���
				if ('1' == relationType) {
					grandChild[grandChildNum] = childName;
					grandChildNum++;
				}
				
				if ('2' == relationType) {
					grandParent[grandParentNum] = parenName;
					grandParentNum++;
				}
				
			}
			System.out.println("grandChildNum="+grandChildNum+"grandParentNum="+grandParentNum);
			// grandchild��grandparent������ѿ�������
			if (grandChildNum != 0 && grandParentNum != 0) {
				for(int m = 0; m < grandChildNum; m++)
				{
					for(int n = 0; n < grandParentNum; n++)
					{
						cont.write(new Text(grandChild[m]), new Text(grandParent[n]));
					}
				}
			}
			
			
		}
	}
	
	//main
	public static void main(String[] args) throws Exception {
		Configuration conf = new Configuration();
		
	    String[] otherArgs = new GenericOptionsParser(conf, args).getRemainingArgs();
		if (otherArgs.length != 2) {
			System.err.println("Usage: Single table join <in>  <out>");
		    System.exit(2);
		}
		
		Job job = new Job(conf, "single table join");
		job.setJarByClass(SingleTableJoin.class);
		job.setMapperClass(Map.class);
		job.setReducerClass(Reduce.class);
		
		job.setOutputKeyClass(Text.class);
		job.setOutputValueClass(Text.class);
		
		FileInputFormat.addInputPath(job, new Path(otherArgs[0]));
		FileOutputFormat.setOutputPath(job, new Path(otherArgs[1]));
		
		System.exit(job.waitForCompletion(true)? 0 : 1);
		
		
	}
}
