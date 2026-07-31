package sdk_examples;

import java.io.File;
import java.util.List;
import javax.xml.bind.JAXBContext;
import javax.xml.bind.Unmarshaller;

// Simulating Clubs package import
import Clubs.ClubDatabase;
import Clubs.Association;
import Clubs.Club;

/**
 * LibreOffice SDK Guide Chapter 50
 * UnmarshallClubs.java - Unmarshalling nested clubs.xml database to Java Object lists.
 */
public class UnmarshallClubs {
    public static void main(String[] args) {
        try {
            JAXBContext jaxbContext = JAXBContext.newInstance(ClubDatabase.class);
            Unmarshaller jaxbUnmarshaller = jaxbContext.createUnmarshaller();
            
            ClubDatabase cd = (ClubDatabase) jaxbUnmarshaller.unmarshal(new File("clubs.xml"));
            List<Association> assocList = cd.getAssociation();

            System.out.println("Associations loaded via JAXB:");
            for (Association assoc : assocList) {
                System.out.println("  Association ID: " + assoc.getId());
                List<Club> clubs = assoc.getClub();
                for (Club club : clubs) {
                    System.out.println("    Club Name: " + club.getName() + " (Charter: " + club.getCharter() + ")");
                    System.out.println("      Contact: " + club.getContact() + ", Phone: " + club.getPhone());
                }
            }
        } catch (Exception e) {
            System.err.println("JAXB Exception: " + e.getMessage());
        }
    }
}
