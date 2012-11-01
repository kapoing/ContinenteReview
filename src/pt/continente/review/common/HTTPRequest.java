package pt.continente.review.common;

import java.io.IOException;
import java.io.InputStream;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;

import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.protocol.BasicHttpContext;
import org.apache.http.protocol.HttpContext;
import org.w3c.dom.Document;
import org.xml.sax.ErrorHandler;
import org.xml.sax.SAXException;
import org.xml.sax.SAXParseException;

import android.os.Bundle;
import android.os.Handler;
import android.os.Message;

public class HTTPRequest extends Thread {
	private static final String TAG = "CntRev - HTTPRequest";

	private String urlBeingSought;
	private HttpResponse response;
	private Handler parentHandler;
	private int requestType;

	public static class requestTypes {
		public static final int GET_ARTICLE = 1;
		public static final int GET_DIMENSIONS = 2;
	}

	public static class responseOutputs {
		public final static int SUCCESS = 10;
		public final static int FAILED_ERROR_ON_SUPPLIED_URL = 11;
		public final static int FAILED_QUERY_FROM_INTERNET = 12;
		public final static int FAILED_GETTING_VALID_RESPONSE_FROM_QUERY = 13;
		public final static int FAILED_PROCESSING_RETURNED_OBJECT = 14;
	}

	public HTTPRequest(Handler parentHandler, String url, int requestType) {
		response = null;
		urlBeingSought = url;
		this.parentHandler = parentHandler;
		this.requestType = requestType;
	}

	@Override
	public void run() {
		super.run();
		Common.log(5, TAG, "run: started");
		String firstChild = null;
		// simulates delay in fetch
		boolean simulateDelay = true;
		try {
			Thread.sleep(simulateDelay ? 2000 : 1);
		} catch (InterruptedException e) {
			Common.log(1, TAG,
					"run: ERROR in applying delay - " + e.getMessage());
			e.printStackTrace();
		}
		Common.log(5, TAG, "run: terminou o delay for�ado");
		Message messageToParent = null;
		DefaultHttpClient client = null;
		HttpContext localContext = null;
		try {
			messageToParent = new Message();
			client = new DefaultHttpClient();
			localContext = new BasicHttpContext();
		} catch (Exception e) {
			e.printStackTrace();
		}
		Common.log(5, TAG, "run: criou as vari�veis chave");

		messageToParent.what = 0;
		response = null;
		HttpGet httpGet = null;

		Common.log(5, TAG, "run: vai criar objeto GET");
		try {
			httpGet = new HttpGet(urlBeingSought);
		} catch (IllegalArgumentException e) {
			Common.log(1, TAG, "run: ERROR in supplied url - " + e.getMessage());
			messageToParent.what = responseOutputs.FAILED_ERROR_ON_SUPPLIED_URL;
		}

		if (messageToParent.what != 0) {
			parentHandler.sendMessage(messageToParent);
			return;
		}

		Common.log(5, TAG, "run: vai obter dados da net");
		try {
			response = client.execute(httpGet, localContext);
		} catch (ClientProtocolException e) {
			Common.log(1, TAG,
					"run: ERROR obtaining response to internet query (ClientProtocolException) - "
							+ e.getMessage());
			messageToParent.what = responseOutputs.FAILED_QUERY_FROM_INTERNET;
		} catch (IOException e) {
			Common.log(1, TAG,
					"run: ERROR obtaining response to internet query (IOException) - "
							+ e.getMessage());
			messageToParent.what = responseOutputs.FAILED_QUERY_FROM_INTERNET;
		} catch (Exception e) {
			Common.log(1, TAG,
					"run: ERROR obtaining response to internet query (UndefinedException) - "
							+ e.getMessage());
			e.printStackTrace();
			messageToParent.what = responseOutputs.FAILED_QUERY_FROM_INTERNET;
		}

		if (messageToParent.what != 0) {
			parentHandler.sendMessage(messageToParent);
			return;
		}

		Common.log(5, TAG, "run: vai processar respostas");
		if (response == null) {
			Common.log(3, TAG, "run: got empty response from query");
			messageToParent.what = responseOutputs.FAILED_GETTING_VALID_RESPONSE_FROM_QUERY;
			parentHandler.sendMessage(messageToParent);
			return;
		} else {
			Common.log(5, TAG,
					"run: vai obter o documento a partir da resposta");
			Document newDocument = null;
			try {

				DocumentBuilderFactory dbf = DocumentBuilderFactory
						.newInstance();

				dbf.setValidating(false);
				// dbf.setNamespaceAware(true);
				dbf.setIgnoringElementContentWhitespace(true);
				DocumentBuilder builder = dbf.newDocumentBuilder();

				builder.setErrorHandler(new ErrorHandler() {
					@Override
					public void error(SAXParseException arg0)
							throws SAXException {
						Common.log(5, TAG, "document builder error 1 ");
						throw arg0;
					}

					@Override
					public void fatalError(SAXParseException arg0)
							throws SAXException {
						Common.log(5, TAG, "document builder error 2 ");
						throw arg0;
					}

					@Override
					public void warning(SAXParseException arg0)
							throws SAXException {
						Common.log(5, TAG, "document builder error 3 ");
						throw arg0;
					}
				});
				Common.log(5, TAG, "run: vai processar entity");
				HttpEntity entity = response.getEntity();
				Common.log(5, TAG, "run: vai processar stream");
				InputStream instream = entity.getContent();
				Common.log(5, TAG, "run: vai processar document");
				newDocument = builder.parse(instream);
				newDocument.normalizeDocument();
				newDocument.normalize();
				firstChild = newDocument.getChildNodes().item(0).getNodeName();
				if (firstChild == null) {
					HTTPResponseException e = new HTTPResponseException(
							"Documento retornado � vazio");
					e.setUrl(urlBeingSought);
					throw e;
				}

				Common.log(5, TAG,
						"run: criar a mensagem a partir do documento");
				Bundle messageData = new Bundle();

				switch (requestType) {
				case requestTypes.GET_ARTICLE:
					Common.log(5, TAG, "run: vai processar artigo");
					if (firstChild.compareTo("article") != 0) {
						HTTPResponseException e = new HTTPResponseException(
								"Documento n�o tem como primeiro elemento um artigo");
						e.setUrl(urlBeingSought);
						throw e;
					}
					Article newArticle = HTTPResponseProcessor
							.getProductFromDoc(newDocument);
					messageData.putSerializable("response", newArticle);
					break;
				case requestTypes.GET_DIMENSIONS:
					Common.log(5, TAG, "run: vai processar dimens�es");
					DimensionsList newDimList = HTTPResponseProcessor
							.getDimensionsFromDoc(newDocument);
					messageData.putSerializable("response", newDimList);
					break;
				}

				Common.log(5, TAG, "run: vai finalizar e enviar mensagem");
				messageToParent.what = responseOutputs.SUCCESS;
				messageToParent.setData(messageData);
				parentHandler.sendMessage(messageToParent);
			} catch (Exception e) {
				Common.log(
						1,
						TAG,
						"run: ERROR processing the returned object - "
								+ e.getMessage());
				messageToParent.what = responseOutputs.FAILED_PROCESSING_RETURNED_OBJECT;
				parentHandler.sendMessage(messageToParent);
				return;
			}
		}
		Common.log(5, TAG, "run: finished");
	}
}
