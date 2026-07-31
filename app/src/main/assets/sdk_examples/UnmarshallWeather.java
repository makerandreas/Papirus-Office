package sdk_examples;

import java.io.File;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import javax.xml.bind.JAXBContext;
import javax.xml.bind.Unmarshaller;
import javax.xml.datatype.XMLGregorianCalendar;

// Simulating Weather package import
import Weather.Current;

/**
 * LibreOffice SDK Guide Chapter 50
 * UnmarshallWeather.java - Unmarshalling and customized schema bindings to resolve name conflicts.
 */
public class UnmarshallWeather {
    public static void main(String[] args) {
        try {
            JAXBContext jaxbContext = JAXBContext.newInstance(Current.class);
            Unmarshaller jaxbUnmarshaller = jaxbContext.createUnmarshaller();
            
            Current currWeather = (Current) jaxbUnmarshaller.unmarshal(new File("weather.xml"));

            // Get precipitation status
            String rainingStatus = currWeather.getPrecipitation().getValue();
            boolean isRaining = rainingStatus.equalsIgnoreCase("yes");

            // Extract Gregorian calendar with conflict-resolved "valueAttribute" binding
            XMLGregorianCalendar gCal = currWeather.getLastupdate().getValueAttribute();
            Calendar cal = gCal.toGregorianCalendar();

            SimpleDateFormat formatter = new SimpleDateFormat("dd/MM/yyyy");
            formatter.setTimeZone(cal.getTimeZone());
            String dateStr = formatter.format(cal.getTime());

            if (isRaining) {
                System.out.println("It was raining on " + dateStr + " (Status: " + rainingStatus + ")");
            } else {
                System.out.println("It was NOT raining on " + dateStr + " (Status: " + rainingStatus + ")");
            }
        } catch (Exception e) {
            System.err.println("JAXB Exception: " + e.getMessage());
            System.err.println("Make sure to include <jaxb:property name=\"valueAttribute\"/> inside weather.xsd to prevent 'value' property conflicts.");
        }
    }
}
