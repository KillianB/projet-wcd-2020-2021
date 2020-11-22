# Projet [TinyGram](https://docs.google.com/document/d/1sFkj4hjT3DBQopovQNor5hylzWZQABk6DlL_HmMPW4M/edit#heading=h.zgqfbizhklet)

Projet réalisé avec Damien MARTIN, Alexandre DESMONTILS et Killian BARREAU.
Le but de ce projet a été de créé une "réplique" de la plateforme Instagram. Ceci est un prétexte pour manipuler Google
Cloud et son App Engine dans le cadre du cours Web & Cloud and Datastores.

## Fonctionnalités implémentées

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
 
Le principal choix d'implémentation qui a été réalisé a été sur les followers. En effet, il n'existe qu'un Kind "Follow",
dont la clé est la personne qui follow et qui a une liste des following. Cette liste peut contenir que 20000 éléments par
la limite du Datastore et nous avons choisi de ne pas créer de FollowIndex.
L'autre choix a été fait sur les likes. Actuellement, il est possible qu'un utilisateur puisse like plusieurs fois un post.
Cela est dû au fait qu'il n'existe pas de kind "Like" qui permettrait de palier à ce problème.
Enfin, il n'est pas possible d'upload une image directement sur notre application mais uniquement fournir un lien vers cette
image pour qu'il soit stocké dans le Datastore.

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
Le servlet qui nous a permis de réaliser les mesures est src/java/tests/getNewPostsMeasure.java. Pour des raisons inconnues il renvoie rarement des résultats.

![screen des resultats obtenus](/screens/screenFromMeasureGetTimeLine.png? "screen des résultats obtenus")


réception de 10 posts : 998 ms \
réception de 100 posts : 959 ms \
réception de 500 posts : 914 ms.

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

