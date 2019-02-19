
package global.cloudcoin.ccbank;

class IncomeFile {

	static int TYPE_JPEG = 1;
	static int TYPE_STACK = 2;

	public IncomeFile(String fileName, int fileType) {
		this.fileName = fileName;
		this.fileType = fileType;
	}

	public String fileName;
	public int fileType;
	public String fileTag;

	
}
