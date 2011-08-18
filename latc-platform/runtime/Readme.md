# VOID FILE EXAMPLE 


The following void example is generated from dbpedia-lgd_island.xml.
@prefix rdf: <http://www.w3.org/1999/02/22-rdf-syntax-ns#> .
@prefix rdfs: <http://www.w3.org/2000/01/rdf-schema#> .
@prefix owl: <http://www.w3.org/2002/07/owl#> .
@prefix void: <http://rdfs.org/ns/void#> .
@prefix : <#> .
:dbpedia a void:Dataset;
        void:sparqlEndpoint <http://live.dbpedia.org/sparql/>;
        .
:linkedgeodata a void:Dataset;
        void:sparqlEndpoint <http://linkedgeodata.org/sparql/>;
        .

:dbpedia2linkedgeodata a void:Linkset ;
        void:linkPredicate owl:sameAs;
        void:target :dbpedia;
         void:target :linkedgeodata ;
        void:triples  9139;

Example of VOID template file

@prefix rdf:    <http://www.w3.org/1999/02/22-rdf-syntax-ns#> .
@prefix rdfs:    <http://www.w3.org/2000/01/rdf-schema#> .
@prefix owl:    <http://www.w3.org/2002/07/owl#> .
@prefix void:             <http://rdfs.org/ns/void#> .
@prefix prv:              <http://purl.org/net/provenance/ns#> .
@prefix prvTypes:         <http://purl.org/net/provenance/types#> .
@prefix xsd:              <http://www.w3.org/2001/XMLSchema#> .
@prefix foaf:             <http://xmlns.com/foaf/0.1> .
@prefix doap:             <http://usefulinc.com/ns/doap#> .
@prefix : <#> .

**newprefix**
:**source** a void:Dataset;
    void:sparqlEndpoint <**sparqlsource**> .
    void:uriLookupEndpoint <**uriLookupsource**> .
:**target** a void:Dataset;
    void:sparqlEndpoint <**sparqltarget**> .
    void:uriLookupEndpoint <**uriLookuptarget**> .
:**linksetname** a void:Linkset ;
    void:linkPredicate **linktype**;
           void:target :**source**;
    void:target :**target**;
    void:triples  **triples**;
    void:dataDump <**datadump**>;
    void:feature <http://www.w3.org/ns/formats/Turtle>;
           a prv:DataItem ;
            prv:createdBy [    a prv:DataCreation ;
                            prv:performedAt "**linksetcreatedtime**"^^xsd:dateTime ;
                    prv:usedData :**source** ;
                    prv:usedData :**target** ;
                           prv:usedGuideline :linkspec;

                    prv:performedBy :SilkMapReduce

                    ] ;

    .

:linkspec a prv:CreationGuideline ;
    a prvTypes:SilkLinkSpecification ;
    prv:createdBy [    a prv:DataCreation ;
                    prv:performedBy <**specauthor**>;
                               prv:performedAt "**speccreatedtime**"^^xsd:dateTime
                          ];
    prv:retrievedBy [    a prv:DataAccess ;
                                    prv:performedAt "**specretrievedtime**"^^xsd:dateTime ;
                            prv:accessedResource   <**specURL**>;          
                                prv:accessedService :console;
                            prv:performedBy :SilkMapReduce
                             ];

.        

prvTypes:SilkLinkSpecification a owl:Class ;
        rdfs:comment "concept that represents the loading and mapping defined in a silk specification file.";
        rdfs:isDefinedBy <http://purl.org/net/provenance/types#> ;
        rdfs:label "Silk Link Specification"@en ;
        rdfs:subClassOf prv:CreationGuideline .
:console a prv:DataProvidingService;
    foaf:homepage  <**consolehost**>.

:SilkMapReduce a  prvTypes:DataCreatingService ;
    prv:deployedSoftware :silkmr .

:silkmr a doap:Version;
        doap:revision "2.3" .

:silkmrProject a doap:Project;
        doap:release :silkmr;
       doap:homepage <http://www4.wiwiss.fu-berlin.de/bizer/silk> .
