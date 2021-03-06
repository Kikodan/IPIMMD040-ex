package com.ipiecoles.java.java230;

import com.ipiecoles.java.java230.exceptions.BatchException;
import com.ipiecoles.java.java230.exceptions.TechnicienException;
import com.ipiecoles.java.java230.model.Commercial;
import com.ipiecoles.java.java230.model.Employe;
import com.ipiecoles.java.java230.model.Manager;
import com.ipiecoles.java.java230.model.Technicien;
import com.ipiecoles.java.java230.repository.EmployeRepository;
import com.ipiecoles.java.java230.repository.ManagerRepository;
import org.joda.time.LocalDate;
import org.joda.time.format.DateTimeFormat;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.CommandLineRunner;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@Component
public class MyRunner implements CommandLineRunner {

    private static final String REGEX_MATRICULE = "^[MTC][0-9]{5}$";
    private static final String REGEX_NOM = ".*";
    private static final String REGEX_PRENOM = ".*";
    private static final int NB_CHAMPS_MANAGER = 5;
    private static final int NB_CHAMPS_TECHNICIEN = 7;
    private static final String REGEX_MATRICULE_MANAGER = "^M[0-9]{5}$";
    private static final int NB_CHAMPS_COMMERCIAL = 7;

    @Autowired
    private EmployeRepository employeRepository;

    @Autowired
    private ManagerRepository managerRepository;

    private List<Employe> employes = new ArrayList<>();

    private final Logger logger = LoggerFactory.getLogger(this.getClass());

    @Override
    public void run(String... strings) throws Exception {
        String fileName = "employes.csv";
        readFile(fileName);
        employeRepository.save(employes);
        //readFile(strings[0]);
    }

    /**
     * Méthode qui lit le fichier CSV en paramètre afin d'intégrer son contenu en BDD
     * @param fileName Le nom du fichier (à mettre dans src/main/resources)
     * @return une liste contenant les employés à insérer en BDD ou null si le fichier n'a pas pu être le
     */
    public List<Employe> readFile(String fileName)  {
        Stream<String> stream;
        logger.info("Lecture du fichier : "+ fileName);

        try {
            stream = Files.lines(Paths.get(new ClassPathResource(fileName).getURI()));
        } catch (IOException e) {
            logger.error("Problèmes dans l'ouverture du fichier" + fileName);
            return new ArrayList<>();
        }
        List<String> lignes = stream.collect(Collectors.toList());
        logger.info(lignes.size()+" lignes lues");
        for (int i = 0; i < lignes.size(); i++){

            try {
                processLine(lignes.get(i));
            } catch (BatchException e) {
                logger.error("Ligne " +(i+1) +" : " +e.getMessage() +" => " +lignes.get(i));
            }

        }

        return employes;
    }

    /**
     * Méthode qui regarde le premier caractère de la ligne et appelle la bonne méthode de création d'employé
     * @param ligne la ligne à analyser
     * @throws BatchException si le type d'employé n'a pas été reconnu
     */
    private void processLine(String ligne) throws BatchException {

        switch (ligne.substring(0,1)){
            case "T" :
                processTechnicien(ligne);
                break;
            case "M" :
                processManager(ligne);
                break;
            case "C" :
                processCommercial(ligne);
                break;
            default :
                throw new BatchException("Type d'employé inconnu");
        }


    }

    /**
     * Méthode qui crée un Commercial à partir d'une ligne contenant les informations d'un commercial et l'ajoute dans la liste globale des employés
     * @param ligneCommercial la ligne contenant les infos du commercial à intégrer
     * @throws BatchException s'il y a un problème sur cette ligne
     */
    private void processCommercial(String ligneCommercial) throws BatchException {

        Commercial C = new Commercial();
        //Controle de matricule
        String[] commercialFields = ligneCommercial.split( ",");

        if (commercialFields.length != NB_CHAMPS_COMMERCIAL){
            throw  new BatchException(" La ligne commercial ne contient pas 7 elements mais " + commercialFields.length);
        }

        infoEmploye(commercialFields, C);

        try {
            C.setCaAnnuel(Double.parseDouble(commercialFields[5]));
        }
        catch (Exception e) {
            throw new   BatchException("Le chiffre d'affaire du commercial est incorrect : " + commercialFields[5]);
        }

        try {
            C.setPerformance(Integer.parseInt(commercialFields[6]));
        }
        catch ( Exception e){
            throw new BatchException("La performance du commercial est incorrecte : "+commercialFields[6] );
        }
    }

    /**
     * Méthode qui crée un Manager à partir d'une ligne contenant les informations d'un manager et l'ajoute dans la liste globale des employés
     * @param ligneManager la ligne contenant les infos du manager à intégrer
     * @throws BatchException s'il y a un problème sur cette ligne
     */
    private void processManager(String ligneManager) throws BatchException {
        String[] managerFields = ligneManager.split(",");

        if (managerFields.length != NB_CHAMPS_MANAGER){
            throw  new BatchException(" La ligne manager ne contient pas 5 elements mais " + managerFields.length);
        }

        Manager M = new Manager();

        infoEmploye(managerFields, M );

    }

    /**
     * Méthode qui crée un Technicien à partir d'une ligne contenant les informations d'un technicien et l'ajoute dans la liste globale des employés
     * @param ligneTechnicien la ligne contenant les infos du technicien à intégrer
     * @throws BatchException s'il y a un problème sur cette ligne
     */
    private void processTechnicien(String ligneTechnicien) throws BatchException {
        String[] technicienFields = ligneTechnicien.split(",");


        if (technicienFields.length != NB_CHAMPS_TECHNICIEN){
            throw  new BatchException(" La ligne technicien ne contient pas 7 elements mais " + technicienFields.length);
        }


        Technicien T = new Technicien();

        try {
            Integer.parseInt(technicienFields[5]);
        }
        catch (Exception e){
            throw new BatchException(( " Le grade "+ technicienFields[5] +" du technicien est incorrect : X => T12345,dupont,pierre,12/03/2003,1700.5,X,M00001"));
        }

        try {
            T.setGrade(Integer.parseInt(technicienFields[5]));
        } catch (TechnicienException e) {
            throw new BatchException("Le grade doit être compris entre 1 et 5 : " + technicienFields[5]);
        }

        infoEmploye(technicienFields, T);

        if (technicienFields[6].matches(REGEX_MATRICULE_MANAGER)){

            if (managerRepository.findByMatricule(technicienFields[6])!= null){
                T.setManager(managerRepository.findByMatricule(technicienFields[6]));
            }
            else {
                throw new BatchException("Le manager de matricule "+technicienFields[6]+" n'a pas été trouvé dans le fichier ou en base de données");
            }

            for (int i = 0; i < employes.size(); i++) {
                if (technicienFields[6].matches(employes.get(i).getMatricule())){
                    T.setManager((Manager) employes.get(i));
                }else {
                    throw new BatchException("Le manager de matricule "+technicienFields[6]+" n'a pas été trouvé dans le fichier ou en base de données");
                }


            }


        }
        else {
            throw new BatchException("la chaîne "+technicienFields[6]+" ne respecte pas l'expression régulière ^M[0-9]{5}$");
        }

    }

    private Employe infoEmploye ( String[] employeFields, Employe E ) throws BatchException{
        if ( !employeFields[0].matches(REGEX_MATRICULE)){
            throw new BatchException("La chaîne "+ employeFields[0] +" ne respecte pas l'expression régulière ^[MTC][0-9]{5}$ ");
        }
        LocalDate D = null ;
        try {
            D = DateTimeFormat.forPattern("dd/MM/yyyy").parseLocalDate(employeFields[3]);
        }
        catch (Exception e) {
            throw new BatchException(employeFields[3] + "ne respecte pas le format de date dd/MM/yyyy");
        }

        try {
            Double.parseDouble(employeFields[4]);
        }

        catch ( Exception e) {
            throw new BatchException(employeFields[4] + " n'est pas un nombre valide pour un salaire");
        }

        E.setMatricule(employeFields[0]);
        E.setNom(employeFields[1]);
        E.setPrenom(employeFields[2]);
        E.setDateEmbauche(D);
        E.setSalaire(Double.parseDouble(employeFields[4]));

        return E;




    }

}


