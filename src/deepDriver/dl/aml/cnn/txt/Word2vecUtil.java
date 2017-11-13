package deepDriver.dl.aml.cnn.txt;


import java.io.BufferedInputStream;
import java.io.DataInputStream;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map.Entry;
import java.util.Set;
import java.util.TreeSet;

//计算词与词之间的相似度
public class Word2vecUtil {
	
	public static void main(String[] args) throws IOException 
	{
//		Word2vecUtil vec = new Word2vecUtil();
//		vec.loadModel("E://data//word2vecResult//vectors.bin");
//
//		
////		//最近邻测试
////		System.out.println("One word analysis：");
////		Set<WordEntry> result = new TreeSet<WordEntry>();
////		result = vec.distance("尸蟞");
////		Iterator iter = result.iterator();
////		while (iter.hasNext()) {
////			WordEntry word = (WordEntry) iter.next();
////			System.out.println(word.name + " " + word.score);
////		}
////		
////		
////		//三词类比测试
////		System.out.println("*******************************");
////		System.out.println("Three word analysis：");
////		result = vec.analogy( "射雕英雄传", "金庸","盗墓笔记");
////		iter = result.iterator();
////		while (iter.hasNext()) 
////		{
////			WordEntry word = (WordEntry) iter.next();
////			System.out.println(word.name + " " + word.score);
////		}	
//		
//		float vector[] = vec.getWordVector("公益");
//		for(int i=0;i<10;i++)
//		{
//			System.out.println(vector[i]);
//		}
		
		System.out.println("sysx");
		
		
	}

	
	//key是词,value是词对应的向量集合
	private HashMap<String, float[]> wordMap = new HashMap<String, float[]>();

	private int words;//词语数量
	private int size;//每一个词对应的向量size
	private int topNSize = 40;//做多关联的数据数量

	/**
	 * 加载模型
	 * 
	 * @param path
	 *            模型的路径
	 * @throws IOException
	 * 存储内容的格式:
	 * 比如一共存储10个词,每一个词的向量size是5,则
	 * 10、5 、词语1、5个float、空格、词语2、5个float等等 
	 */
	public void loadModel(String path) throws IOException 
	{
		DataInputStream dis = null;
		BufferedInputStream bis = null;
		double len = 0;
		float vector = 0;
		try {
			bis = new BufferedInputStream(new FileInputStream(path));
			dis = new DataInputStream(bis);
			// //读取词数
			words = Integer.parseInt(readString(dis));
			// //大小
			size = Integer.parseInt(readString(dis));//向量大小

			String word;
			float[] vectors = null;
			for (int i = 0; i < words; i++) {//循环读取每一个词
				word = readString(dis);//读取词
				vectors = new float[size];
				len = 0;
				for (int j = 0; j < size; j++) {
					vector = readFloat(dis);//临时向量的一个值
					len += vector * vector;
					vectors[j] = (float) vector;
				}
				len = Math.sqrt(len);

				for (int j = 0; j < vectors.length; j++) {
					vectors[j] = (float) (vectors[j] / len);
				}
				wordMap.put(word, vectors);
				dis.read();//过滤空格
			}

		} finally {
			bis.close();
			dis.close();
		}
	}

	private static final int MAX_SIZE = 50;

	/**
	 * 得到近义词
	 * 
	 * @param word
	 * @return
	 */
	public Set<WordEntry> distance(String word) 
	{
		float[] wordVector = getWordVector(word);//获取该词语的向量
		if (wordVector == null) 
		{
			return null;
		}
		
		//计算该词与所有的词的距离
		Set<Entry<String, float[]>> entrySet = wordMap.entrySet();//所有的词
		float[] tempVector = null;
		List<WordEntry> wordEntrys = new ArrayList<WordEntry>(topNSize);//存储最接近的词语集合
		String name = null;
		for (Entry<String, float[]> entry : entrySet) {//循环每一个词
			name = entry.getKey();
			if (name.equals(word)) {//词就是本身
				continue;
			}
			float dist = 0;//总的距离值
			tempVector = entry.getValue();//每一个词的向量
			for (int i = 0; i < wordVector.length; i++) {//计算向量距离
				dist += wordVector[i] * tempVector[i];
			}
			insertTopN(name, dist, wordEntrys);
		}
		return new TreeSet<WordEntry>(wordEntrys);
	}
	
	/**
	 * 计算两个词向量的相似度（内积）
	 * @param word1
	 * @param word2
	 * @return  Similarity
	 */
	public float similarity(String word1, String word2)
	{
		float[] wordVector1 = getWordVector(word1);
		float[] wordVector2 = getWordVector(word2);
		
		if (wordVector1 == null||wordVector2==null) 
		{
			return -1;
		}
		float sim= 0;
		for(int i = 0; i < wordVector1.length; i++)
		{
			sim += wordVector1[i] * wordVector2[i];
		}
		
		return sim;
	}
	
	
	
	/**
	 * 计算两个向量相似度
	 * @param wordVector1
	 * @param wordVector2
	 * @return
	 */
	public float vectorSimilarity(float[] wordVector1, float[] wordVector2)
	{		
		if (wordVector1 == null||wordVector2==null) 
		{
			return -1;
		}
		float sim= 0;
		for(int i = 0; i < wordVector1.length; i++)
		{
			sim += wordVector1[i] * wordVector2[i];
		}		
		return sim;
	}
	
	
	/**
	 * 词与词进行类比  (A+B-a=b)
	 * 
	 * @return
	 */
	public TreeSet<WordEntry> analogy(String word0, String word1, String word2) 
	{
		float[] wv0 = getWordVector(word0);
		float[] wv1 = getWordVector(word1);
		float[] wv2 = getWordVector(word2);

		if (wv1 == null || wv2 == null || wv0 == null) {
			return null;
		}
		float[] wordVector = new float[size];
		for (int i = 0; i < size; i++) {
			wordVector[i] = wv1[i] - wv0[i] + wv2[i];
		}
		float[] tempVector;
		String name;
		List<WordEntry> wordEntrys = new ArrayList<WordEntry>(topNSize);
		for (Entry<String, float[]> entry : wordMap.entrySet()) {
			name = entry.getKey();
			if (name.equals(word0) || name.equals(word1) || name.equals(word2)) {
				continue;
			}
			float dist = 0;
			tempVector = entry.getValue();
			for (int i = 0; i < wordVector.length; i++) {
				dist += wordVector[i] * tempVector[i];
			}
			insertTopN(name, dist, wordEntrys);
		}
		return new TreeSet<WordEntry>(wordEntrys);
	}

	
	
	//这部分使用优先队列更好
	private void insertTopN(String name, float score, List<WordEntry> wordsEntrys) 
	{
		if (wordsEntrys.size() < topNSize) {
			wordsEntrys.add(new WordEntry(name, score));
			return;
		}
		float min = Float.MAX_VALUE;//最小的分数
		int minOffe = 0;//最小分数在集合的index
		for (int i = 0; i < topNSize; i++) {
			WordEntry wordEntry = wordsEntrys.get(i);
			if (min > wordEntry.score) {
				min = wordEntry.score;
				minOffe = i;
			}
		}

		if (score > min) {//分数比最小的大.则存储到该位置
			wordsEntrys.set(minOffe, new WordEntry(name, score));
		}
	}

	
	public class WordEntry implements Comparable<WordEntry> 
	{
		public String name;
		public float score;

		public WordEntry(String name, float score) {
			this.name = name;
			this.score = score;
		}

		@Override
		public String toString() {
			return this.name + "\t" + score;
		}

		@Override
		public int compareTo(WordEntry o) {
			if (this.score > o.score) {
				return -1;
			} else {
				return 1;
			}
		}

	}

	/**
	 * 得到词向量
	 * 
	 * @param word
	 * @return
	 */
	public float[] getWordVector(String word) 
	{
		return wordMap.get(word);
	}

	//读取四个字节
	public static float readFloat(InputStream is) throws IOException 
	{
		byte[] bytes = new byte[4];
		is.read(bytes);
		return getFloat(bytes);
	}

	/**
	 * 读取一个float
	 * 从字节数组中读取4个字节,组成float
	 * @param b
	 * @return
	 */
	public static float getFloat(byte[] b) 
	{
		int accum = 0;
		accum = accum | (b[0] & 0xff) << 0;
		accum = accum | (b[1] & 0xff) << 8;
		accum = accum | (b[2] & 0xff) << 16;
		accum = accum | (b[3] & 0xff) << 24;
		return Float.intBitsToFloat(accum);
	}

	/**
	 * 读取一个字符串
	 * 遇见换行或者空格,则停止读取
	 * @param dis
	 * @return
	 * @throws IOException
	 */
	private static String readString(DataInputStream dis) throws IOException 
	{
		byte[] bytes = new byte[MAX_SIZE];//读取临时的数据
		byte b = dis.readByte();//读取一个字节
		int i = -1;
		StringBuilder sb = new StringBuilder();
		while (b != 32 && b != 10) {//不是空格,也不是换行符号
			i++;
			bytes[i] = b;
			b = dis.readByte();
			if (i == 49) {//说明数组已经满了,要先写入进去,重新初始化一个空的数组
				sb.append(new String(bytes));
				i = -1;
				bytes = new byte[MAX_SIZE];
			}
		}
		sb.append(new String(bytes, 0, i + 1,"UTF-8"));
		return sb.toString();
	}

	public int getTopNSize() 
	{
		return topNSize;
	}

	public void setTopNSize(int topNSize) 
	{
		this.topNSize = topNSize;
	}

	public HashMap<String, float[]> getWordMap() 
	{
		return wordMap;
	}

	public int getWords() 
	{
		return words;
	}

	public int getSize() 
	{
		return size;
	}
}