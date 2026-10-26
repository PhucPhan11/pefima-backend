package com.ceu.pefima.backend.market;

import java.io.IOException;
import java.io.StringReader;
import java.math.BigDecimal;
import java.net.URI;
import java.time.Clock;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

import javax.xml.XMLConstants;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;

import org.jspecify.annotations.Nullable;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;
import org.xml.sax.ErrorHandler;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;
import org.xml.sax.SAXParseException;

import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

/**
 * Reads Vietcombank's public exchange-rate XML. Rates there use a comma as the
 * thousands separator, and a rate the bank does not quote is {@code "-"}.
 */
class VietcombankRateClient implements ExchangeRateClient {

    private static final DateTimeFormatter DATE_TIME = DateTimeFormatter.ofPattern("M/d/yyyy h:mm:ss a", Locale.US);

    private static final ErrorHandler RETHROW = new ErrorHandler() {

        @Override
        public void warning(SAXParseException ex) {
        }

        @Override
        public void error(SAXParseException ex) throws SAXException {
            throw ex;
        }

        @Override
        public void fatalError(SAXParseException ex) throws SAXException {
            throw ex;
        }

    };

    private final RestClient restClient;

    private final URI rateUrl;

    private final Clock clock;

    VietcombankRateClient(RestClient restClient, URI rateUrl, Clock clock) {
        this.restClient = restClient;
        this.rateUrl = rateUrl;
        this.clock = clock;
    }

    @Override
    public FxQuote fetch() {
        String body;
        try {
            body = this.restClient.get().uri(this.rateUrl).retrieve().body(String.class);
        }
        catch (RestClientException ex) {
            throw new MarketPriceUnavailableException("Could not read the Vietcombank exchange-rate list", ex);
        }
        if (body == null || body.isBlank()) {
            throw new MarketPriceUnavailableException("The Vietcombank exchange-rate list was empty");
        }
        Document document = parse(body);
        List<FxQuote.Row> rows = new ArrayList<>();
        NodeList rates = document.getElementsByTagName("Exrate");
        for (int i = 0; i < rates.getLength(); i++) {
            Element rate = (Element) rates.item(i);
            String code = rate.getAttribute("CurrencyCode").trim();
            if (!code.isEmpty()) {
                rows.add(new FxQuote.Row(code, rate.getAttribute("CurrencyName").trim(), rate(rate, "Buy"),
                        rate(rate, "Transfer"), rate(rate, "Sell")));
            }
        }
        if (rows.isEmpty()) {
            throw new MarketPriceUnavailableException("The Vietcombank exchange-rate list has no rates");
        }
        return new FxQuote(List.copyOf(rows), dateTime(document), this.clock.instant());
    }

    private static Document parse(String body) {
        try {
            DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
            factory.setFeature(XMLConstants.FEATURE_SECURE_PROCESSING, true);
            factory.setFeature("http://apache.org/xml/features/disallow-doctype-decl", true);
            factory.setXIncludeAware(false);
            factory.setExpandEntityReferences(false);
            DocumentBuilder builder = factory.newDocumentBuilder();
            builder.setErrorHandler(RETHROW);
            return builder.parse(new InputSource(new StringReader(body)));
        }
        catch (ParserConfigurationException | SAXException | IOException ex) {
            throw new MarketPriceUnavailableException("The Vietcombank exchange-rate list is not valid XML", ex);
        }
    }

    private static @Nullable BigDecimal rate(Element element, String attribute) {
        String text = element.getAttribute(attribute).trim().replace(",", "");
        if (text.isEmpty() || text.equals("-")) {
            return null;
        }
        try {
            BigDecimal rate = new BigDecimal(text);
            return rate.signum() > 0 ? rate : null;
        }
        catch (NumberFormatException ex) {
            return null;
        }
    }

    private static @Nullable LocalDateTime dateTime(Document document) {
        NodeList nodes = document.getElementsByTagName("DateTime");
        if (nodes.getLength() == 0) {
            return null;
        }
        try {
            return LocalDateTime.parse(nodes.item(0).getTextContent().trim(), DATE_TIME);
        }
        catch (DateTimeParseException ex) {
            return null;
        }
    }

}
