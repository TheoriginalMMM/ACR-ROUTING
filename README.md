[![Build Status](https://travis-ci.org/matsim-org/matsim-code-examples.svg?branch=0.9.x)](https://travis-ci.org/matsim-org/matsim-code-examples)

# matsim-code-examples
A repository containing code examples around MATSim
### To understeand the aim of this project:
You find a Vilguarisation video [here] (https://drive.google.com/file/d/1FKPkjKrzFByyfWnAEkZ8QQ613KTGDguJ/view?usp=sharing)

### Abstract : 

Avec la croissance du nombre de véhicules partout dans le monde, une meilleure gestion des embouteillages reste parmi les préoccupations continues des chercheurs dans le domaine du transport. Le but de ce projet de recherche est d'implémenter un algorithme de routage dynamique capable de détecter les congestions et de bien rediriger les voitures dans des situations de congestion. En nous intéressant à l'algorithme de colonies de fourmis ACO, nous avons développé deux variantes (FRV et FRN) de ce dernier basé sur l’usage de deux types de phéromones (attractive et répulsive). Nos variantes ont été implémentées et ajoutées à MaTSim (outil de simulation dans le domaine du transport) afin de les tester sur différentes simulations. Les deux versions de notre algorithme ont montré des bonnes performances en général avec une légère supériorité pour FRN. Nous avons également constaté que nos versions présentent des limites dans l’évitement les congestions sur certaines routes du réseau : les routes qui mènent directement à une destination. Des améliorations possibles de notre implémentation ont été proposées dans la conclusion. 

Mots-clés : Algorithme de routage dynamique ; Algorithme de colonies de fourmis ACO ; Phéromone répulsive ; Phéromone attractive ; MaTSim

Notes :
- FRV : Une version basée sur le temps passé sur chaque lien.
- FRN : Une version basée sur le nombre de voitures sur chaque lien.
- MaTSim : Un cadre de travail (framework en anglais), open-source pour la mise en œuvre de simulations de transport à grande échelle basées sur des agents.
Auteur :
Mohamed El Mehdi MAKHLOUF - étudiant en master 1 Inforamtique, Université Claude Bernard Lyon 1, mohamed-el-mehdi.makhlouf@etu.univ-lyon1.fr
Encadré par :
Salima HASAS - Professeur des universités, Université Claude Bernard Lyon 1, salima.hassas@univ-lyon1.fr.
Cécile DANIEL - Doctorant , LICIT, cecile.daniel@entpe.fr.


### Notes
You find the implementation of ACR in "/src/main/java/tutorial/withinday/withinDayReplanningAgents/"
#### Versioning
The default branch is the current release.  So currently (January 2017) the release is 0.9.x, so the default branch of this project here is 0.9.x.  Once we release 0.10.x, 0.9.x should remain in place, but the default branch should be moved to 0.10.x.  The hope is that we get somewhat more stable links in q&a, because if someone browses on github, (s)he will do this in the default branch, and thus in most cases copy/paste that default branch link into the q&a.
