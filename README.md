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
 - Lister ses follows
 - Lister ses followers
 - Afficher sa timeline
 <!-- - TODO: Unlike un post-->
 
Le principal choix d'implémentation qui a été réalisé a été sur les followers. En effet, il n'existe qu'un Kind 
"Follow", dont la clé est la personne qui follow et qui a une liste des following. Cette liste peut contenir que 20000 
éléments par la limite du Datastore et nous avons choisi de ne pas créer de FollowIndex. L'autre choix a été fait sur 
les likes. Actuellement, il est possible qu'un utilisateur puisse like plusieurs fois un post. Cela est dû au fait qu'il
n'existe pas de kind "Like" qui permettrait de palier à ce problème. Enfin, il n'est pas possible d'upload une image 
directement sur notre application mais uniquement fournir un lien vers cette image pour qu'il soit stocké dans le 
Datastore.

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

Création d'un message avec 10 followers : 179 ms \
Création d'un message avec 100 followers : 197 ms \
Création d'un message avec 500 followers : 470 ms

![screen des benckmark des posts](/screens/screenFromMeasurePost.png "")

### Afficher les posts

Le second benchmark est le suivant : combien de temps prends l'affichage des posts d'une personne par 10, 100 ou 500 
posts ? Le servlet qui nous a permis de réaliser les mesures est src/java/tests/getNewPostsMeasure.java. Pour des 
raisons inconnues il renvoie rarement des résultats.

![screen des resultats obtenus](/screens/screenFromMeasureGetTimeLine.png "screen des résultats obtenus")


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
 - User  

### Post

Ce kind a été crée pour pouvoir gérer les actions liées aux posts et pouvoir les convertir facilement en objets 
utilisables en java.
Les attributs sont les suivants :
 - sender : renseigne l'identité de la personne à l'origine du post
 - url : permet d'associer une image à un post défini par son url
 - body : l'attribut qui contient le corps du message.

### PostIndex

Ce kind est directement associé au message d'origine. Il permet de renseigner les personnes à qui on envoie le messages 
(ici les followers). Il permet au destinataire de recevoir le message d'origine directement grâce à l'index 
automatiquement généré par google cloud.

### LikeCounter

Ce kind est un type d'entité qui permet de récupérer le nombre de likes du messages avec un minimum de concurrence.
Pour chaque message nous en créons 10 et lorsque qu'un utilisateur like le post, un des 10 compteurs sera choisi 
aléatoirement pour sauvegarder le like en cours. Par conséquent jusqu'à 10 personnes en même temps peuvent like un même 
post.

### Follow

Ce kind permet de faire le lien entre un utilisateur et un autre par un lien d'abonnement. En effet, si user1 "follow" 
user2, alors user1 recevra les messages de user2 lorsque celui-ci va en poster.

### User

Ce kind nous permet de stocker dans le datastore les nom et photo de profil des utilisateurs de l'application. Ces 
éléments seront affichés sur le message envoyé par l'utilisateur.

## Conclusion

Ce projet est complexe à réaliser car il a fallu réfléchir à des moyens de rendre efficace les requêtes et méthodes 
d'API. Ceux-ci n'ont pas été facile à mettre en oeuvre surtout à cause des problèmes liés aux clés. Nous avons eu des 
difficultés lors de l'élaboration des servlets de mesures d'efficacité des get et post. En effet, pour une raison
inconnue, les valeurs que nous obtenons sont très élevées voire aberrantes. L'utilisation de mithril a posé problème
surtout sur la gestion des requêtes, des comportements inattendus et compliqués à corriger.

Sinon comme possibilités d'améliorer l'application, la possibilité d'associer un like à un utilisateur pour un post 
donné permettrait de rajouter la possibilité de unlike un post et que chaque utilisateur puisse like au plus une fois.
En ce qui concerne le front, un refactoring serait pas mal pour augmenter la lisibilité du code et sa manipulation. Une
meilleur compréhension des Promise de mithril.js pour mieux gérer les requêtes effectuées pour intéragir correctement
selon les réponses.


