# Projet [TinyGram](https://docs.google.com/document/d/1sFkj4hjT3DBQopovQNor5hylzWZQABk6DlL_HmMPW4M/edit#heading=h.zgqfbizhklet)

Projet réalisé avec Damien MARTIN, Alexandre DESMONTILS et Killian BARREAU.
Le but de ce projet a été de créé une "réplique" de la plateforme Instagram. Ceci est un prétexte pour manipuler Google
Cloud et son App Engine dans le cadre du cours Web & Cloud and Datastores.

## Fonctionnalités implémentées

TODO: Choix d'implémentation

 - Login avec Google (OAuth)
 - Page de profil avec ses posts
 - Ajouter un nouveau post
 - Follow quelqu'un
 - Unfollow quelqu'un
 - Like un post
 - TODO: Unlike un post
 - Lister ses follows
 - Listes ses followers
 - Afficher sa timeline

## URLs

 - URL de l'application : [TinyGram](https://tinyinsta-295118.ew.r.appspot.com/)  
 - URL du GitHub : [Github](https://github.com/KillianB/projet-wcd-2020-2021)
    - FulgurentKille : Damien MARTIN
    - KillianB : Killian BARREAU
    - adesmontils : Alexandre DESMONTILS
 - URL interface REST : [Interface REST](https://endpointsportal.tinyinsta-295118.cloud.goog)

## Benchmarks

Afin de pouvoir tester notre solution, des benchmarks ont été réalisé selon les spécifications données dans le document
présentant le projet. Tout les benchmarks ont été réalisé sur 30 mesures pondérées.

### Ajouter un post

Le premier benchmark est le suivant : en fonction du nombre de followers d'une personne (10, 100, 500), combien de temps
prends la création d'un message ?

Création d'un message avec 10 followers : 
Création d'un message avec 100 followers : 
Création d'un message avec 500 followers : 

### Afficher les posts

Le second benchmark est le suivant : combien de temps prends l'affichage des posts d'une personne par 10, 100 ou 500 posts ?

Création d'un message avec 10 posts : 
Création d'un message avec 100 posts : 
Création d'un message avec 500 posts : 

### Like par seconde

Le dernier benchmark est le suivant : combien de like par seconde est-il possible de réaliser ?

X likes par secondes : 
X likes par secondes : 
X likes par secondes : 

## Les kinds utilisés

 - Post
 - PostIndex
 - LikeCounter
 - Follow  

TODO

## Conclusion
TODO

