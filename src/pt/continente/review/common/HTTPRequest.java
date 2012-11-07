package pt.continente.review.common;

import java.io.BufferedReader;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.List;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;

import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.mime.HttpMultipartMode;
import org.apache.http.entity.mime.MultipartEntity;
import org.apache.http.entity.mime.content.ByteArrayBody;
import org.apache.http.entity.mime.content.StringBody;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.protocol.BasicHttpContext;
import org.apache.http.protocol.HttpContext;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Bitmap.CompressFormat;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;

public class HTTPRequest extends Thread {
	private static final String TAG = "CntRev - HTTPRequest";

	private Context context;
	private String urlBeingSought;
	private HttpResponse response;
	private Handler parentHandler;
	private int requestType;

	public static class requestTypes {
		public static final int GET_ARTICLE = 1;
		public static final int GET_DIMENSIONS = 2;
	}

	public static class responseOutputs {
		public final static int SUCCESS = 5;
		public final static int FAILED_NO_NETWORK_CONNECTION_DETECTED = 10;
		public final static int FAILED_ERROR_ON_SUPPLIED_URL = 11;
		public final static int FAILED_QUERY_FROM_INTERNET = 12;
		public final static int FAILED_GETTING_VALID_RESPONSE_FROM_QUERY = 13;
		public final static int FAILED_PROCESSING_RETURNED_OBJECT = 14;
		public final static int FAILED_OBJECT_NOT_FOUND = 15;
	}

	public HTTPRequest(Context context, Handler parentHandler, String url, int requestType) {
		this.context = context;
		this.parentHandler = parentHandler;
		this.urlBeingSought = url;
		this.requestType = requestType;
		response = null;
	}

	@Override
	public void run() {
		super.run();
		Common.log(5, TAG, "run: started");

		Message messageToParent = new Message();

		if (!Common.isNetworkConnected(context)) {
			Common.log(1, TAG, "run: no network connection present");
			messageToParent.what = responseOutputs.FAILED_NO_NETWORK_CONNECTION_DETECTED;
			parentHandler.sendMessage(messageToParent);
			return;
		}

		DefaultHttpClient client = null;
		HttpContext localContext = null;
		try {
			client = new DefaultHttpClient();
			localContext = new BasicHttpContext();
		} catch (Exception e) {
			e.printStackTrace();
		}

		messageToParent.what = 0;
		response = null;

		HttpGet httpGet = null;
		try {
			httpGet = new HttpGet(urlBeingSought);
		} catch (IllegalArgumentException e) {
			Common.log(1, TAG, "run: ERROR in supplied url - " + e.getMessage());
			messageToParent.what = responseOutputs.FAILED_ERROR_ON_SUPPLIED_URL;
			parentHandler.sendMessage(messageToParent);
			return;
		}

		Common.log(5, TAG, "run: vai obter dados da net");
		try {
			response = client.execute(httpGet, localContext);
		} catch (ClientProtocolException e) {
			messageToParent.what = responseOutputs.FAILED_QUERY_FROM_INTERNET;
		} catch (IOException e) {
			messageToParent.what = responseOutputs.FAILED_QUERY_FROM_INTERNET;
		} catch (Exception e) {
			messageToParent.what = responseOutputs.FAILED_QUERY_FROM_INTERNET;
			e.printStackTrace();
		}

		if (messageToParent.what != 0) {
			parentHandler.sendMessage(messageToParent);
			return;
		}
		if (response == null) {
			Common.log(3, TAG, "run: got empty response from query");
			messageToParent.what = responseOutputs.FAILED_GETTING_VALID_RESPONSE_FROM_QUERY;
			parentHandler.sendMessage(messageToParent);
			return;
		} else {
			Common.log(5, TAG, "run: obteve resposta; vai tentar processar");
			Document newDocument = null;
			try {
				DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
				dbf.setValidating(false);
				dbf.setIgnoringElementContentWhitespace(true);
				DocumentBuilder builder = dbf.newDocumentBuilder();

				HttpEntity entity = response.getEntity();
				InputStream instream = entity.getContent();
				newDocument = builder.parse(instream);
				newDocument.normalizeDocument();
				newDocument.normalize();

				Common.log(5, TAG, "run: recebeu xml v�lido; vai verificar conte�do");
				Node node = newDocument.getDocumentElement().getChildNodes().item(0);
				if (node.getNodeName().compareTo("error") == 0) {
					messageToParent.what = responseOutputs.FAILED_OBJECT_NOT_FOUND;
					Bundle messageData = new Bundle();
					messageData.putString("errorMessage", node.getTextContent());
					messageToParent.setData(messageData);
					parentHandler.sendMessage(messageToParent);
					return;
				}

				Common.log(5, TAG, "run: conte�do � v�lido, vai processar");
				switch (requestType) {
				case requestTypes.GET_ARTICLE:
					Article newArticle = HTTPRequest.getProductFromDoc(newDocument);
					messageToParent.obj = newArticle;
					break;
				case requestTypes.GET_DIMENSIONS:
					List<Dimension> newDimList = getDimensionsFromDoc(newDocument);
					messageToParent.obj = newDimList;
					break;
				}
				messageToParent.what = responseOutputs.SUCCESS;
				parentHandler.sendMessage(messageToParent);
			} catch (Exception e) {
				Common.log(1, TAG, "run: ERROR processing the returned object - " + e.getMessage());
				messageToParent.what = responseOutputs.FAILED_PROCESSING_RETURNED_OBJECT;
				parentHandler.sendMessage(messageToParent);
				return;
			}
		}
		Common.log(5, TAG, "run: finished");
	}

	public static List<Dimension> getDimensionsFromDoc(Document document) {
		Common.log(5, TAG, "getDimensions: started");

		if (document == null) {
			return null;
		}

		Element root;
		NodeList dimensions;
		NodeList dimensionNodes;
		List<Dimension> returnList = new ArrayList<Dimension>();

		document.getDocumentElement().normalize();
		root = document.getDocumentElement();
		dimensions = root.getChildNodes();

		Common.log(5, TAG, "getDimensions: found '" + dimensions.getLength() + "' elements in response");

		Node proxNode;
		Node proxDimension;
		long id = 0;
		String name = "";
		String label = "";
		String min = "";
		String med = "";
		String max = "";

		// str = str.replaceAll("[0-9]", "X");
		for (int i = 0; i < dimensions.getLength(); i++) {
			proxDimension = dimensions.item(i);
			dimensionNodes = proxDimension.getChildNodes();
			for (int j = 0; j < dimensionNodes.getLength(); j++) {
				proxNode = dimensionNodes.item(j);
				if (proxNode.getNodeName().compareTo("id") == 0) {
					id = Long.parseLong(proxNode.getTextContent());
				}
				if (proxNode.getNodeName().compareTo("name") == 0) {
					name = proxNode.getTextContent();
				}
				if (proxNode.getNodeName().compareTo("labelDimension") == 0) {
					label = proxNode.getTextContent();
				}
				if (proxNode.getNodeName().compareTo("labelMin") == 0) {
					min = proxNode.getTextContent();
				}
				if (proxNode.getNodeName().compareTo("labelMed") == 0) {
					med = proxNode.getTextContent();
				}
				if (proxNode.getNodeName().compareTo("labelMax") == 0) {
					max = proxNode.getTextContent();
				}
			}
			Common.log(5, TAG, "" + id + ":" + name + "." + label + "*" + min + ";;" + med + ";;;" + max);
			returnList.add(new Dimension(id, name, label, min, med, max));
		}
		Common.log(5, TAG, "getDimensions: built an array with '" + returnList.size() + "' elements");
		Common.log(5, TAG, "getDimensions: finished");
		return returnList;
	}

	public static Article getProductFromDoc(Document document) {

		if (document == null) {
			return null;
		}

		Element root;
		NodeList nodeList;

		document.getDocumentElement().normalize();
		root = document.getDocumentElement();
		nodeList = root.getChildNodes();

		Node proxNode;
		String id = "-1";
		String name = "";
		String description = "Description";
		String productEan = "";
		String price = "";
		String urlImg = "";
		String prodStructL1 = "";
		String prodStructL2 = "";
		String prodStructL3 = "";
		String prodStructL4 = "";

		for (int i = 0; i < nodeList.getLength(); i++) {
			proxNode = nodeList.item(i);
			if (proxNode.getNodeName().compareTo("id") == 0) {
				id = proxNode.getTextContent();
			}
			if (proxNode.getNodeName().compareTo("name") == 0) {
				name = proxNode.getTextContent();
			}
			if (proxNode.getNodeName().compareTo("ean") == 0) {
				productEan = proxNode.getTextContent();
			}
			if (proxNode.getNodeName().compareTo("price") == 0) {
				price = proxNode.getTextContent();
			}
			if (proxNode.getNodeName().compareTo("urlImg") == 0) {
				urlImg = proxNode.getTextContent();
			}
			if (proxNode.getNodeName().compareTo("prodStructL1") == 0) {
				prodStructL1 = proxNode.getTextContent();
			}
			if (proxNode.getNodeName().compareTo("prodStructL2") == 0) {
				prodStructL2 = proxNode.getTextContent();
			}
			if (proxNode.getNodeName().compareTo("prodStructL3") == 0) {
				prodStructL3 = proxNode.getTextContent();
			}
			if (proxNode.getNodeName().compareTo("prodStructL4") == 0) {
				prodStructL4 = proxNode.getTextContent();
			}

		}
		Common.log(3, TAG, "Name(String):" + name);
		Article gettedArticle = new Article(Long.parseLong(id), name, description, productEan, Double.parseDouble(price), urlImg, null, Integer.parseInt(prodStructL1), Integer.parseInt(prodStructL2), Integer.parseInt(prodStructL3), Integer.parseInt(prodStructL4));
		Common.log(3, TAG, "" + gettedArticle);
		return gettedArticle;
	}

	// FIXME Isto nao pode ficar no HTTPRequest
	// O HTTPRequest assume que tem que fazer um GET e depois trata um XML, que
	// n�o e o caso aqui

	// FIXME Isto foi testado umas ZERO vezes, que � mais ou menos a
	// probabilidade de funcionar � primeira
	public void submitReview(Review review, Article article, Bitmap bitmap) throws Exception {
		try {

			HttpClient httpClient = new DefaultHttpClient();
			HttpContext localContext = new BasicHttpContext();
			HttpPost httpPost = new HttpPost(Common.httpVariables.REVIEW_PREFIX);
			MultipartEntity entity = new MultipartEntity(HttpMultipartMode.BROWSER_COMPATIBLE);
			Bitmap bmpCompressed = Bitmap.createScaledBitmap(bitmap, 640, 480, true);
			ByteArrayOutputStream bos = new ByteArrayOutputStream();
			bmpCompressed.compress(CompressFormat.JPEG, 100, bos);
			byte[] data = bos.toByteArray();
			entity.addPart("Article_name", new StringBody(article.getName()));
			// TO DO and so on and so on, para tudo o que define um artigo...

			// sending a Image;
			// note here, that you can send more than one image, just add
			// another param, same rule to the String;

			entity.addPart("Review_image1", new ByteArrayBody(data, "imagem1.jpg"));
			// TO DO and so on and so on, para todas as imagens

			httpPost.setEntity(entity);
			HttpResponse response = httpClient.execute(httpPost, localContext);
			BufferedReader reader = new BufferedReader(new InputStreamReader(response.getEntity().getContent(), "UTF-8"));
			String sResponse = reader.readLine();
			sResponse.compareTo("warningW");

		} catch (Exception e) {

			Common.log(5, TAG, "Erro a fazer upload de review");
			Common.log(5, TAG, "" + e);
			Common.longToast(this.context, "Erro a fazer upload de review");
		}

	}

}
