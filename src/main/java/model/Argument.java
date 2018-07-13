package model;

import org.apache.commons.cli.*;

public class Argument {
	private int size = 1;
	private int numOfIdentifierWorker = 1;
	private int numOfBestModelFinderWorker = 1;
	private int numOfBuilderWorker = 1;
	private boolean isParallel = false;
	private boolean isProduction = false;
	
	public Argument(String[] args) throws Exception{
		Options options = new Options();
		Option size = new Option("s", "size", true, "num of months");
		Option parallel = new Option("p", "parallel", false, "is parallel");
		Option identifier = new Option("i", "identifier", true, "num of identifier worker");
		Option bestFinder = new Option("bf", "best-finder", true , "num of best model finder worker");
		Option builder = new Option("b", "builder", true, "num of pool model builder");
		
        size.setRequired(false);
        parallel.setRequired(false);
        identifier.setRequired(false);
        bestFinder.setRequired(false);
        builder.setRequired(false);
        
        options.addOption(size);
        options.addOption(parallel);
        options.addOption(identifier);
        options.addOption(bestFinder);
        options.addOption(builder);
        
        CommandLineParser parser = new DefaultParser();
        CommandLine cmd = parser.parse(options, args);
        
        String sizeArg = cmd.getOptionValue("size").toLowerCase();
        char sizeFormat = sizeArg.charAt(sizeArg.length()-1);
        int sizeNumber = Integer.parseInt(sizeArg.substring(0, sizeArg.length()-1));
    
        if (sizeFormat == 'h'){
        	this.size = sizeNumber;
        } else if (sizeFormat == 'd'){
        	this.size = sizeNumber * 24;
        } else if (sizeFormat == 'w'){
        	this.size = sizeNumber * 24 * 7;
        } else if (sizeFormat == 'm'){
        	this.size = sizeNumber * 24 * 30;
        } else if (sizeFormat == 'y'){
        	this.size = sizeNumber * 24 * 30 * 12;
        }
        
        this.isParallel = cmd.hasOption("parallel");

        if (this.isParallel){
        	this.numOfIdentifierWorker = cmd.hasOption("identifier") ? Integer.parseInt(cmd.getOptionValue("identifier")) : 0;
        	this.numOfBestModelFinderWorker = cmd.hasOption("best-finder") ? Integer.parseInt(cmd.getOptionValue("best-finder")) : 0;
        	this.numOfBuilderWorker = Integer.parseInt(cmd.getOptionValue("builder"));
        }       
	}

	public int getSize() {
		return size;
	}

	public int getNumOfIdentifierWorker() {
		return numOfIdentifierWorker;
	}

	public int getNumOfBestModelFinderWorker() {
		return numOfBestModelFinderWorker;
	}

	public int getNumOfBuilderWorker() {
		return numOfBuilderWorker;
	}

	public boolean isParallel() {
		return isParallel;
	}
	
	public boolean isProduction(){
		return isProduction;
	}
	
	@Override
	public String toString(){
		String format = "%d %d %d %d %s %s";
	
		return String.format(format, size, numOfIdentifierWorker, numOfBestModelFinderWorker, numOfBuilderWorker, 
				isParallel, isProduction);
	}
}
