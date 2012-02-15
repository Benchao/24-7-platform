package com.deri.latc.utility;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.fs.FSDataInputStream;
import org.apache.hadoop.fs.FSDataOutputStream;
import org.apache.hadoop.fs.FileSystem;
import org.apache.hadoop.fs.Path;
import org.apache.hadoop.fs.FileUtil;
import org.apache.hadoop.io.IOUtils;

/**
 * Hadoop File System management
 * 
 * @author Nur Aini Rakhmawati
 * @since February 2011
 */

public class HadoopClient {

	private final String User;
	private static FileSystem hdfs = null;
	private String ErrorMessage = null;
	private final Configuration conf = new Configuration();

	/**
	 * 
	 * @param hadoopPath
	 *            path of hadoop installation
	 */

	public HadoopClient(final String hadoopPath) {
		this(hadoopPath, System.getProperty("user.name"));
	}

	/**
	 * 
	 * @param hadoopPath
	 *            path of hadoop installation
	 * @param hadoopUser
	 *            user of hadoop
	 */
	public HadoopClient(final String hadoopPath, final String hadoopUser) {
		this.User = hadoopUser;

		if (hdfs == null) {
			try {

				conf.addResource(new Path(hadoopPath + "/conf/core-site.xml"));
				conf.addResource(new Path(hadoopPath + "/conf/hdfs-site.xml"));
				hdfs = FileSystem.get(conf);
			} catch (IOException ex) {
				System.err.print(ex.getMessage());
			}
		}
	}

	/**
	 * Create file in HDFS
	 * 
	 * @param path
	 *            path of file in HDFS
	 * @param content
	 *            content to be stored
	 * @param replace
	 *            true if replacing existing file
	 * @return true if create file successfully
	 */
	public boolean createFile(String path, String content, boolean replace) {
		boolean create = false;
		final Path pathfile = new Path("/user/" + this.User + '/' + path);
		if (this.exists(pathfile) && replace)
			this.deleteFile(path);
		if ((this.exists(pathfile) && replace) || !this.exists(pathfile)) {
			FSDataOutputStream dos;
			try {
				dos = hdfs.create(pathfile);
				for (int i = 0; i < content.length(); i++)
					dos.write(content.charAt(i));
				dos.close();
				create = true;
			} catch (IOException e) {
				this.ErrorMessage = e.getMessage();
			}
		}

		return create;
	}

	/**
	 * Creating directory in HDFS
	 * 
	 * @param path
	 *            path of directory in HDFS
	 * @return true if creating directory successfully
	 */
	public boolean createDir(String path) {
		boolean create = false;
		final Path pathfile = new Path("/user/" + this.User + '/' + path);
		System.out.println(pathfile.toString());
		if (!this.exists(pathfile))
			try {
				if (hdfs.mkdirs(pathfile))
					create = true;

			} catch (IOException e) {
				this.ErrorMessage = e.getMessage();
			}

		return create;
	}

	/**
	 * Deleting file in HDFS
	 * 
	 * @param path
	 *            path of file in HDFS
	 * @return true if creating file successfully
	 */
	public boolean deleteFile(String path) {
		boolean delete = false;
		final Path pathfile = new Path("/user/" + this.User + '/' + path);
		if (this.exists(pathfile))
			try {
				hdfs.delete(pathfile, false);
				delete = true;
			} catch (IOException e) {
				this.ErrorMessage = e.getMessage();
			}

		return delete;
	}

	/**
	 * Deleting directory in HDFS
	 * 
	 * @param path
	 *            path of directory in HDFS
	 * @return true if creating file successfully
	 */
	public boolean deleteDir(String path) {
		boolean delete = false;
		final Path pathDir = new Path("/user/" + this.User + '/' + path);
		if (this.exists(pathDir))
			try {
				hdfs.delete(pathDir, true);
				delete = true;
			} catch (IOException e) {
				this.ErrorMessage = e.getMessage();
			}

		return delete;
	}

	/**
	 * Merging two file to be one file
	 * 
	 * @param src1
	 *            path of first file
	 * @param src2
	 *            path of second file
	 * @param des
	 *            path of destination file, if not assigned, the second source
	 *            become default destination
	 * @return true if merging done
	 */
	public boolean mergeFile(String src1, String src2, String des) {
		boolean create = false;
		final Path pathsrc1 = new Path("/user/" + this.User + '/' + src1);
		final Path pathsrc2 = new Path("/user/" + this.User + '/' + src2);

		StringBuffer buffer = new StringBuffer();
		try {
			FSDataInputStream dis1 = hdfs.open(pathsrc1);
			int data = dis1.read();
			while (data > 0) {
				buffer.append((char) data);
				data = dis1.read();

			}
			dis1.close();

			FSDataInputStream dis2 = hdfs.open(pathsrc2);
			data = dis2.read();
			if (data != -1)
				buffer.append("\n");
			while (data > 0) {
				buffer.append((char) data);
				data = dis2.read();
			}
			dis2.close();

			if (des != null)
				create = this.createFile(des, buffer.toString(), true);
			else
				create = this.createFile(src2, buffer.toString(), true);
			buffer.setLength(0);

		} catch (IOException e) {
			this.ErrorMessage = e.getMessage();
		}
		return create;
	}

	/**
	 * Checking existing directory or file
	 * 
	 * @param path
	 *            path of directory or file
	 * @return true if file or directory is exist
	 */
	public boolean exists(Path path) {
		try {
			return hdfs.exists(path);
		} catch (IOException ex) {
			this.ErrorMessage = ex.getMessage();
		}
		return false;
	}

	/**
	 * Testing the available of HDFS
	 * 
	 * @return true if HDFS is ready to use
	 */
	public boolean test() {
		boolean check = false;
		final Path pathfile = new Path("/user/" + this.User + "/test");
		check = this.createFile(pathfile.toString(), "hadoop", true);
		this.deleteFile(pathfile.toString());
		return check;
	}

	/**
	 * close the HDFS
	 */
	public void close() {
		try {
			hdfs.close();
		} catch (IOException ex) {
			this.ErrorMessage = ex.getMessage();
		}
	}

	/**
	 * Getting error message
	 * 
	 * @return null if no error
	 */
	public String getMessage() {
		return this.ErrorMessage;

	}

	/**
	 * Copying from local filesystem to HDFS <br/>
	 * 
	 * @param srclocal
	 *            local file path
	 * @param destfs
	 *            HDFS file path
	 * @return
	 */
	public boolean copyFromLocalFile(String srclocal, String destfs) {
		final Path pathsrc = new Path(srclocal);
		final Path pathdes = new Path("/user/" + this.User + '/' + destfs);
		boolean copy = false;
		try {
			hdfs.copyFromLocalFile(pathsrc, pathdes);
			copy = true;
		} catch (IOException e) {
			this.ErrorMessage = e.getMessage();
		}
		return copy;
	}

	/**
	 * Copying from HDFS to local filesystem
	 * 
	 * @param srchdfs
	 *            path file in HDFS
	 * @param destlocal
	 *            path file in local
	 * @return
	 */
	public boolean copyToLocalFile(String srchdfs, String destlocal) {
		final Path pathsrc = new Path(srchdfs);
		final Path pathdes = new Path("/user/" + this.User + '/' + destlocal);
		boolean copy = false;
		try {
			hdfs.copyToLocalFile(pathsrc, pathdes);
			copy = true;
		} catch (IOException e) {
			this.ErrorMessage = e.getMessage();
		}
		return copy;
	}

	/**
	 * Copying and merging several file in the HDFS to one local filesystem <br/>
	 * You could assign regex files
	 * 
	 * @param srchdfs
	 *            several files in one directory <br/>
	 *            example : exampledir/* or exampledir/*.txt
	 * @param dstlocal
	 *            the local path file result
	 * @param endline
	 *            true if each file is separated by line
	 */
	public void copyMergeToLocal(String srchdfs, String dstlocal, boolean endline) {

		final Path srcPath = new Path("/user/" + this.User + '/' + srchdfs);
		final Path desPath = new Path(dstlocal);
		try {
			Path[] srcs = FileUtil.stat2Paths(hdfs.globStatus(srcPath), srcPath);
			OutputStream out = FileSystem.getLocal(conf).create(desPath);
			for (int i = 0; i < srcs.length; i++) {
				System.out.println(srcs[i]);
				InputStream in = hdfs.open(srcs[i]);

				IOUtils.copyBytes(in, out, conf, false);
				if (endline)
					out.write(("\n").getBytes("UTF-8"));
				in.close();

			}
			out.close();

		} catch (IOException ex) {
			this.ErrorMessage = ex.getMessage();
		}
	}

	/**
	 * @param args
	 * @throws IOException
	 */
	public static void main(String[] args) throws IOException {
		// HadoopClient HC = new HadoopClient("hadoop-0.20.2", "nurgun");

		// HC.close();
	}

}
