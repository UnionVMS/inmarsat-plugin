package eu.europa.ec.fisheries.uvms.plugins.inmarsat.telnetserversimulator;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;

public class DNIDHandler {

	public static final String END = "\r\n";
	private static final Logger LOGGER = LoggerFactory.getLogger(DNIDHandler.class);

	private String dnid;
	private String area;
	String dnidRoot = null;
	String arguments;

	public DNIDHandler(String arguments) {
		this.arguments = arguments;
	}

	private boolean parseArguments() {

		String parts[] = arguments.split(" ");
		if (parts.length < 2) {
			return false;
		}
		if (parts.length >= 1) {
			dnid = parts[0];
		}
		if (parts.length >= 2) {
			area = parts[1];
		} else {
			area = "1";

		}
		return true;
	}

	public boolean verify() {

		if (!parseArguments()) {
			LOGGER.error("arguments not ok . Check your call");
			return false;
		}

		return true;
	}

	public Response execute() {

		File folder = new File(dnidRoot);

		File[] dnidFiles = folder.listFiles();

		// for every file in the catalog

		ByteArrayOutputStream bos = new ByteArrayOutputStream();
		try {
			for (File file : dnidFiles) {
				byte[] data = Files.readAllBytes(file.toPath());
				bos.write(data);
			}
			bos.close();
			return new Response(bos.toByteArray());

		} catch (IOException e) {
			return new Response();
		}

	}
}
