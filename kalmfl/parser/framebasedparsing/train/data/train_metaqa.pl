train('Actors appear in the films.','Movie',index(1,2),[pair('Movie_name',index(1,5),required),pair('Actor',index(1,1),required)],['act','star'],'').
train('Actors are someones in the films.','Movie',index(2,1),[pair('Movie_name',index(2,6),required),pair('Actor',index(2,1),required)],[],'').
train('Director\\\'s films.','Movie',index(3,1),[pair('Movie_name',index(3,3),required),pair('Director',index(3,1),required)],[],'').
train('Directors directed the films.','Movie',index(4,2),[pair('Movie_name',index(4,4),required),pair('Director',index(4,1),required)],['co_direct'],'').
train('Films are directed by directors.','Movie',index(5,3),[pair('Movie_name',index(5,1),required),pair('Director',index(5,5),required)],[],'').
train('Films are starred by actors.','Movie',index(6,3),[pair('Movie_name',index(6,1),required),pair('Actor',index(6,5),required)],['act'],'').
train('Films are starred by the actors in the films.','Movie',index(7,6),[pair('Movie_name',index(7,9),required),pair('Actor',index(7,6),required)],[],'').
train('Films starred actors.','Movie',index(8,2),[pair('Movie_name',index(8,1),required),pair('Actor',index(8,3),required)],[],'').
train('Films share actors with the films.','Movie',index(9,3),[pair('Movie_name',index(9,6),required),pair('Actor',index(9,3),required)],[],'').
train('Films share actors with the films.','Movie',index(10,3),[pair('Movie_name',index(10,1),required),pair('Actor',index(10,3),required)],[],'').
train('Films share actors with the films.','Inequality',index(11,2),[pair('Entity_1',index(11,1),required),pair('Entity_2',index(11,6),required)],[],'').
train('Films share directors with the films.','Movie',index(12,3),[pair('Movie_name',index(12,6),required),pair('Director',index(12,3),required)],[],'').
train('Films share directors with the films.','Movie',index(13,3),[pair('Movie_name',index(13,1),required),pair('Director',index(13,3),required)],[],'').
train('Films share writers with the films.','Movie',index(14,3),[pair('Movie_name',index(14,6),required),pair('Writer',index(14,3),required)],[],'').
train('Films share writers with the films.','Movie',index(15,3),[pair('Movie_name',index(15,1),required),pair('Writer',index(15,3),required)],[],'').
train('Films were in the genres.','Movie',index(16,5),[pair('Movie_name',index(16,1),required),pair('Genre',index(16,5),required)],[],'').
train('Films were released in 2021.','Movie',index(17,3),[pair('Movie_name',index(17,1),required),pair('Release_year',index(17,5),required)],[],'').
train('Film whose actors appear in the films.','Movie',index(18,3),[pair('Movie_name',index(18,1),required),pair('Actor',index(18,3),required)],[],'').
train('Film whose actors also appear in the films.','Inequality',index(19,4),[pair('Entity_1',index(19,1),required),pair('Entity_2',index(19,8),required)],['act','star'],'').
train('Film whose directors directed the films.','Movie',index(20,3),[pair('Movie_name',index(20,1),required),pair('Director',index(20,3),required)],[],'').
train('Film whose directors also directed the films.','Inequality',index(21,4),[pair('Entity_1',index(21,1),required),pair('Entity_2',index(21,7),required)],['write'],'').
train('Film whose writers wrote the films.','Movie',index(22,3),[pair('Movie_name',index(22,1),required),pair('Writer',index(22,3),required)],['screenwriter','scriptwriter'],'').
train('Films are written by writers.','Movie',index(23,3),[pair('Movie_name',index(23,1),required),pair('Writer',index(23,5),required)],[],'').
train('Film actors.','Movie',index(24,2),[pair('Movie_name',index(24,1),required),pair('Actor',index(24,2),required)],[],'').
train('Film directors.','Movie',index(25,2),[pair('Movie_name',index(25,1),required),pair('Director',index(25,2),required)],[],'').
train('Film writers.','Movie',index(26,2),[pair('Movie_name',index(26,1),required),pair('Writer',index(26,2),required)],['screenwriter','scriptwriter'],'').
train('Genres of the films.','Movie',index(27,1),[pair('Movie_name',index(27,4),required),pair('Genre',index(27,1),required)],[],'').
train('Languages are English in the films.','Movie',index(28,1),[pair('Movie_name',index(28,6),required),pair('Language',index(28,3),required)],[],'').
train('Languages are spoken in the films.','Movie',index(29,3),[pair('Movie_name',index(29,6),required),pair('Language',index(29,1),required)],[],'').
train('Films are the genres.','Movie',index(30,4),[pair('Movie_name',index(30,1),required),pair('Genre',index(30,4),required)],['type'],'').
train('Films fall under the genres.','Movie',index(31,5),[pair('Movie_name',index(31,1),required),pair('Genre',index(31,5),required)],[],'').
train('Films are in the languages.','Movie',index(32,5),[pair('Movie_name',index(32,1),required),pair('Language',index(32,5),required)],[],'').
train('Release dates of the films.','Movie',index(33,2),[pair('Movie_name',index(33,5),required),pair('Release_year',index(33,2),required)],['year'],'').
train('The films released sometime.','Movie',index(34,3),[pair('Movie_name',index(34,2),required),pair('Release_year',index(34,4),required)],[],'').
train('Directors directed the films for the writers.','Movie',index(35,2),[pair('Movie_name',index(35,4),required),pair('Writer',index(35,7),required)],[],'').
train('Actors starred the films for the directors.','Movie',index(36,2),[pair('Movie_name',index(36,4),required),pair('Director',index(36,7),required)],[],'').
train('Writers wrote the films.','Movie',index(37,2),[pair('Movie_name',index(37,4),required),pair('Writer',index(37,1),required)],['co_write'],'').
train('Writer\\\'s films.','Movie',index(38,1),[pair('Movie_name',index(38,3),required),pair('Writer',index(38,1),required)],['screenwreiter','scriptwriter'],'').
train('Actors starred the films.','Movie',index(39,2),[pair('Movie_name',index(39,4),required),pair('Actor',index(39,1),required)],['act'],'').
train('Actors of the films.','Movie',index(40,1),[pair('Movie_name',index(40,4),required),pair('Actor',index(40,1),required)],[],'').
train('Directors of the films.','Movie',index(41,1),[pair('Movie_name',index(41,4),required),pair('Director',index(41,1),required)],[],'').
train('Writers of the films.','Movie',index(42,1),[pair('Movie_name',index(42,4),required),pair('Writer',index(42,1),required)],['screenwreiter','scriptwriter'],'').
train('Actors in the films also appear in the films.','Inequality',index(43,5),[pair('Entity_1',index(43,4),required),pair('Entity_2',index(43,9),required)],['act','star'],'').
train('Directors of the films also directed the films.','Inequality',index(44,5),[pair('Entity_1',index(44,4),required),pair('Entity_2',index(44,8),required)],['write'],'').
train('Film will release whose directors directed the films.','Movie',index(45,5),[pair('Movie_name',index(45,1),required),pair('Director',index(45,5),required)],[],'').
train('Film will release whose also directors directed the films.','Inequality',index(46,5),[pair('Entity_1',index(46,1),required),pair('Entity_2',index(46,9),required)],['write'],'').
train('Film will release whose writers wrote the films.','Movie',index(47,5),[pair('Movie_name',index(47,1),required),pair('Writer',index(47,5),required)],[],'').
train('Film will release whose actors appear in the films.','Movie',index(48,5),[pair('Movie_name',index(48,1),required),pair('Actor',index(48,5),required)],[],'').
train('Film will release whose actors also appear in the films.','Inequality',index(49,6),[pair('Entity_1',index(49,1),required),pair('Entity_2',index(49,10),required)],[],'').
train('Films are directed by the director of the films.','Movie',index(50,3),[pair('Movie_name',index(50,1),required),pair('Director',index(50,6),required)],[],'').
train('Films are directed by the same director of the films.','Inequality',index(51,6),[pair('Entity_1',index(51,1),required),pair('Entity_2',index(51,10),required)],['write'],'').
train('Films have actors of the films.','Movie',index(52,3),[pair('Movie_name',index(52,1),required),pair('Actor',index(52,3),required)],[],'').
train('Films have the same actors of the films.','Inequality',index(53,4),[pair('Entity_1',index(53,1),required),pair('Entity_2',index(53,7),required)],['share'],'').
train('Films have writers of the films.','Movie',index(54,3),[pair('Movie_name',index(54,1),required),pair('Writer',index(54,3),required)],['screenwriter','scriptwriter'],'').
train('The [MASK]-starred movies.','Movie',index(55,6),[pair('Movie_name',index(55,7),required),pair('Actor',index(55,3),required)],['act'],'').
train('The [MASK]-directed films.','Movie',index(56,6),[pair('Movie_name',index(56,7),required),pair('Director',index(56,3),required)],[],'').
train('The [MASK]-written films.','Movie',index(57,6),[pair('Movie_name',index(57,7),required),pair('Writer',index(57,3),required)],[],'').
train('Films have actors of the films.','Movie',index(58,3),[pair('Movie_name',index(58,1),required),pair('Actor',index(58,3),required)],[],'').
train('Films have the same actors of the films.','Inequality',index(59,4),[pair('Entity_1',index(59,1),required),pair('Entity_2',index(59,8),required)],['share'],'').
train('Films have director of the films.','Movie',index(60,3),[pair('Movie_name',index(60,1),required),pair('Director',index(60,3),required)],[],'').
train('Films have writers of the films.','Movie',index(61,3),[pair('Movie_name',index(61,1),required),pair('Writer',index(61,3),required)],['screenwriter','scriptwriter'],'').
train('The co-writers of the writer.','Coop',index(62,2),[pair('Writer',index(62,5),required),pair('Writer',index(62,2),required)],[],'').
train('The co-directors of the director.','Coop',index(63,2),[pair('Director',index(63,5),required),pair('Director',index(63,2),required)],[],'').
train('Actors of directors [MASK].','Coop',index(64,3),[pair('Director',index(64,3),required),pair('Actor',index(64,1),required)],[],'').
train('Actors of writers [MASK].','Coop',index(65,3),[pair('Writer',index(65,3),required),pair('Actor',index(65,1),required)],['screenwriter','scriptwriter'],'').
train('Writers wrote the screenplays for the movies.','Movie',index(66,2),[pair('Movie_name',index(66,7),required),pair('Writer',index(66,1),required)],[],'').
train('Actors in the films.','Movie',index(67,1),[pair('Movie_name',index(67,4),required),pair('Actor',index(67,1),required)],[],'').
train('Actors appear in the films with actors.','Movie',index(68,2),[pair('Movie_name',index(68,5),required),pair('Actor',index(68,7),required)],[],'').
train('Actors appear in the same films with actors.','Inequality',index(69,5),[pair('Entity_1',index(69,1),required),pair('Entity_2',index(69,8),required)],[],'').
train('Actors co-starred with actors.','Coop',index(70,2),[pair('Actor',index(70,4),required),pair('Actor',index(70,1),required)],['act','star'],'').
train('Actor is a co-star of actor.','Coop',index(71,4),[pair('Actor',index(71,6),required),pair('Actor',index(71,1),required)],[],'').
train('Director of films is also the director of films.','Inequality',index(72,5),[pair('Entity_1',index(72,3),required),pair('Entity_2',index(72,9),required)],[],'').
train('Directors co-directed films with directors.','Movie',index(73,2),[pair('Movie_name',index(73,3),required),pair('Director',index(73,5),required)],['direct'],'').
train('Directors co-directed films with directors.','Inequality',index(74,2),[pair('Entity_1',index(74,1),required),pair('Entity_2',index(74,5),required)],['direct','co_write','write'],'').
train('Writers co-write films with writers.','Movie',index(75,2),[pair('Movie_name',index(75,3),required),pair('Writer',index(75,5),required)],['write'],'').
train('Actors of films also appear in the films.','Inequality',index(76,4),[pair('Entity_1',index(76,3),required),pair('Entity_2',index(76,8),required)],['act','star'],'').
train('When are the movies?','Movie',index(77,1),[pair('Movie_name',index(77,4),required),pair('Release_year',index(77,1),required)],[],'').
train('Actors starred the movies for the writers.','Movie',index(78,2),[pair('Movie_name',index(78,4),required),pair('Writer',index(78,7),required)],[],'').
train('Films share writers with the films.','Inequality',index(79,2),[pair('Entity_1',index(79,1),required),pair('Entity_2',index(79,6),required)],[],'').
train('Someone is listed as the director of the film.','Movie',index(80,6),[pair('Movie_name',index(80,9),required),pair('Director',index(80,1),required)],[],'').
train('Someone is listed as the writer of the film.','Movie',index(81,6),[pair('Movie_name',index(81,9),required),pair('Writer',index(81,1),required)],['screenwriter','scriptwriter'],'').
train('What genres are films?','Movie',index(82,2),[pair('Movie_name',index(82,4),required),pair('Genre',index(82,2),required)],[],'').
