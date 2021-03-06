import java.io.*;
import java.util.Scanner;
import java.nio.charset.StandardCharsets;
/**
 * Primary class that takes a .GBK file and outputs a BTree file.
 * It is able to implement a cache feature, and convert a resulting string of characters into a long.
 * The long converts into the B-tree key, with the string turning into the value paired with the resultant key.
 * 2 arrays store the key and the value pairings, and then a for loop iterates through both arrays to create the BTree.
 * @Author Michael Alberda
 * @Author Jordan Whyte
 * @Author Alayne Rice
 * @Semester Fall 2021
 * @Limitations The Degree of the BTree must be greater than 4 and even.
 */
public class GeneBankCreateBTree<T> implements Serializable
{
	//open file and read only atgc characters after origin before // line
	public static void main(String args[])
	{
		long start =System.currentTimeMillis();
		int cacheLevel=Integer.parseInt(args[0]);//debug level
		int degree=Integer.parseInt(args[1]);//degree of tree
		int cacheSize = 0;
		if(degree<4 || degree%2==1)
		{
			System.out.println("Degree must be greater than 4 and even");
			return;
		}
		String gbk=(args[2]);//input file
		int k=Integer.parseInt(args[3]);//sequence length

		try {
			cacheSize = Integer.parseInt(args[4]);
		}catch(Exception e){System.out.println("No chosen cache size");}

		int debugLevel=0;
		try {
			debugLevel = Integer.parseInt(args[5]);
		}catch(Exception e){System.out.println("no second debug level");}


		if(k>31)
		{
			System.out.println("size must be smaller than 31!");
			return;
		}

		String str=getFullString(gbk);				//gets full string of actgACTG
		String strArr[]=getCutStrings(str, k);		//cuts the previous string into k sized chunks
		long intArr[]=getLongInts(strArr, k);		//converts the previous string into binary, then long int based off that

		BTree bTree=new BTree(degree);

		//if using cache create the btree and put data into the cache
		if(cacheLevel == 1)
		{
			Cache cache = new Cache(cacheSize);

			for (int i=0; i<intArr.length; i++) {
				if(cache.find(intArr[i]) == null)
				{
					bTree.put(intArr[i], strArr[i]);
				}
				else
				{
					bTree.searchIncrementFrequency(bTree.getRoot(), intArr[i], bTree.getHeight());
				}
			}
		}
		else
		{
			for (int i=0; i<intArr.length; i++){
				bTree.put(intArr[i],strArr[i] );
			}
		}

		gbk=gbk.substring(5,14);
		if(debugLevel==1)
		{
			System.out.println("Making a dump file gbk is "+gbk);
			try {
				File file=new File("output/"+gbk+".btree.dump."+k);
				if (file.createNewFile()) {
					System.out.println("File created: " + "output/" + gbk + ".btree.dump." + k);
				} else {
					System.out.println("File exists already.");
				}
				byte[]data=bTree.toString().getBytes(StandardCharsets.UTF_8);
				try(FileOutputStream fos=new FileOutputStream(file)) {
					fos.write(data);
					System.out.println("successfully completed");
				}catch(IOException e){
					e.printStackTrace();
				}

			}catch(IOException e){
				System.out.println("An error occured when creating dump file");
				e.printStackTrace();
			}
		}

		System.out.println();
		//System.out.println(bTree);
		System.out.println();
		System.out.println("size:    " + bTree.size());
		System.out.println("height:  " + bTree.getHeight());
		long finish=System.currentTimeMillis();
		System.out.println("Time elapsed: "+(finish-start));

		try{
			File file=new File("output/"+gbk+".btree.data."+k+"."+degree);
			FileOutputStream fos = new FileOutputStream(file);
			ObjectOutputStream oos = new ObjectOutputStream(fos);
			oos.writeObject(bTree);
			oos.close();
		}catch(Exception e){System.out.println("something went wrong duirng serialization");}
	}




	//no idea if this is needed

	/**
	 * Creates an array of long values that will be used as the keys.
	 * @param strArr the array of strings that are made from
	 * @param k Length of the strings that are being input from the array.
	 * @return
	 */
	public static long [] getLongInts(String []strArr,int k)
	{
		try{
			String []strInt=new String[strArr.length];
			long toRetLong[]=new long[strArr.length];
			for(int j=0;j<strArr.length;j++)
			{
				strInt[j]="";
				for(int i=0;i<strArr[0].length();i++)
				{							//there has to be a more efficent method but im tired and dont want to fix this.
					if(strArr[j].charAt(i)=='A' || strArr[j].charAt(i)=='a')
					{
						strInt[j]=strInt[j]+"00";
					}
					else if(strArr[j].charAt(i)=='T' || strArr[j].charAt(i)=='t')
					{
						strInt[j]=strInt[j]+"11";
					}
					else if(strArr[j].charAt(i)=='C' || strArr[j].charAt(i)=='c')
					{
						strInt[j]=strInt[j]+"01";
					}
					else
					{
						strInt[j]=strInt[j]+"10";
					}
				}
				toRetLong[j]=Long.parseLong(strInt[j],2);
				//	System.out.println("j is "+j);				//testing
				//	System.out.println(strInt[j]);
				//	System.out.println(toRetLong[j]);
			}
			return toRetLong;

		}catch(Exception e)
		{
			System.out.println("something went wrong converting the strings to longs");
		}
		return null;
	}

	/**
	 * Creates an array of cut strings that will be used in the B-Tree as the values.
	 * Utilizes the getFullString file to cut into the array of strings with size k.
	 * @param str the String
	 * @param k Lenfth of the stroi
	 * @return an array of strings.
	 */
	public static String [] getCutStrings(String str, int k)
	{
		String [] cutStrings=new String[str.length()-k];
		for(int i=0;i<str.length()-k;i++)
		{
			cutStrings[i]=str.substring(i,i+k);
		}
		return cutStrings;
	}

	/**
	 * Takes the complete GBK file, and removes everything but ATCG from the file, as one massive string.
	 * @param gbk The file to be input to be scrubbed into a workable file.
	 * @return
	 */
	public static String getFullString(String gbk)
	{
		try
		{

			String str;		//final string with only actg in it
			str="";

			try (Scanner sc=new Scanner(new File(gbk))) 	//read first argument as file name
			{

				String line;		//temp string name for reading input file
				Boolean dna=false;	//if true, reading dna into str String
				while (sc.hasNextLine())
				{
					Scanner sc2=new Scanner(sc.nextLine());
					while(sc2.hasNext())
					{

						line=sc2.next();
						if(!dna)
						{
							if(line.equals("ORIGIN"))
							{
								dna=true;
//								System.out.println("found origin");
							}
						}
						else if(dna)
						{				//ends dna reading segments
							if(line.equals("//"))
							{
								dna=false;
//								System.out.println("found //");		//when it finds // it ends dna mode
								continue;
							}
							else
							{

								for(int i=0;i<line.length();i++)
								{
									//removes numbers and 'n's from it hopefully. kinda annoying but idk it works
									if(line.charAt(i)!='g' && line.charAt(i)!='a' && line.charAt(i)!='c' && line.charAt(i)!='t' && line.charAt(i)!='G' && line.charAt(i)!='A' && line.charAt(i)!='C' && line.charAt(i)!='T')
										continue;

									else 						//if a or g or c or t, adds to str
										str=str+line.charAt(i);

								}
							}
						}
					}
				}
			}catch (Exception e)
			{
				System.out.println("Failed to read file "+gbk);
			}
			return str;
		}catch(Exception e){
			System.out.println("Input in style java GeneBankCreateBTree [inputfile] [length]");
		}
		return "";
	}
}