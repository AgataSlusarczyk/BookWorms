-- Sample books for local development: the books the app showed before it used the server.
-- Loaded by DevDataLoader on every start of ./gradlew bootRun. Books that are already there are skipped.
--
-- The first six ISBNs are the ones the app used. The app had no ISBN for the other seven, so they get
-- made-up ones (978000000000x) – swap them for real editions when we pick them.
-- Page counts are approximate.
-- No semicolons inside the texts: the file is split into statements on them.

INSERT INTO books (source, isbn, title, authors, publisher, published_year, page_count, description) VALUES
    ('MANUAL', '9788308049461', 'Solaris', 'Stanisław Lem', 'Wydawnictwo Literackie', 1961, 224,
     'Stacja badawcza nad oceanem planety, który odpowiada na ludzkie pytania w sposób niemożliwy do rozszyfrowania.'),
    ('MANUAL', '9788308057619', 'Niezwyciężony', 'Stanisław Lem', 'Wydawnictwo Literackie', 1964, 240,
     'Załoga krążownika Niezwyciężony szuka zaginionego statku na planecie Regis III i trafia na chmury maleńkich maszyn, które przetrwały tam bez ludzi.'),
    ('MANUAL', '9788308060411', 'Opowieści o pilocie Pirxie', 'Stanisław Lem', 'Wydawnictwo Literackie', 1968, 320,
     'Opowiadania o drodze Pirxa od niezdarnego kadeta do doświadczonego komandora, który w kosmosie częściej mierzy się z ludźmi i maszynami niż z obcymi.'),
    ('MANUAL', '9788373271897', 'Lalka', 'Bolesław Prus', NULL, 1890, 680,
     'Kupiec Stanisław Wokulski dorabia się majątku, żeby zdobyć arystokratkę Izabelę Łęcką. Do tego panorama Warszawy końca XIX wieku.'),
    ('MANUAL', '9788375780638', 'Problem trzech ciał', 'Cixin Liu', NULL, 2008, 440,
     'W czasie rewolucji kulturalnej z Chin wysłano sygnał w kosmos. Odpowiedź przychodzi po latach i zmienia losy całej ludzkości.'),
    ('MANUAL', '9788375780659', 'Wiedźmin', 'Andrzej Sapkowski', 'SuperNOWA', 1994, 280,
     'Opowiadania o Geralcie z Rivii, łowcy potworów, który w świecie pełnym uprzedzeń trzyma się własnego kodeksu.'),
    ('MANUAL', '9780000000001', 'Lód', 'Jacek Dukaj', NULL, 2007, 1050,
     'Rok 1924, Warszawa wciąż skuta Lodem po wybuchu tunguskim. Benedykt Gierosławski jedzie Transsibem na Syberię szukać ojca, który podobno rozmawia z lutymi.'),
    ('MANUAL', '9780000000002', 'Bieguni', 'Olga Tokarczuk', NULL, 2007, 450,
     'Powieść z luźno połączonych opowieści o podróżowaniu, ruchu i ludzkim ciele. Nagroda Nike 2008.'),
    ('MANUAL', '9780000000003', 'Kwiaty dla Algernona', 'Daniel Keyes', NULL, 1966, 300,
     'Charlie Gordon przechodzi eksperymentalną operację, która podnosi jego inteligencję. Historia opowiedziana w jego własnych raportach.'),
    ('MANUAL', '9780000000004', 'Pachnidło', 'Patrick Süskind', NULL, 1985, 270,
     'Jean-Baptiste Grenouille rodzi się bez własnego zapachu, za to z najczulszym węchem w XVIII-wiecznej Francji i obsesją stworzenia doskonałych perfum.'),
    ('MANUAL', '9780000000005', 'Czarna Ziemia', 'Timothy Snyder', NULL, 2015, 500,
     'Historia Zagłady pokazana jako skutek zniszczenia państw i instytucji w Europie Wschodniej.'),
    ('MANUAL', '9780000000006', 'Mistrz i Małgorzata', 'Michaił Bułhakow', NULL, 1967, 500,
     'Szatan Woland odwiedza Moskwę lat 30. Obok toczy się historia Mistrza, jego powieści o Poncjuszu Piłacie i kochającej go Małgorzaty.'),
    ('MANUAL', '9780000000007', 'Sto lat samotności', 'Gabriel García Márquez', NULL, 1967, 450,
     'Siedem pokoleń rodu Buendía w miasteczku Macondo: kronika miłości, wojen i samotności w duchu realizmu magicznego.')
ON CONFLICT (isbn) DO NOTHING;

INSERT INTO book_genres (book_id, genre_id)
SELECT b.id, g.id
FROM (VALUES
    ('9788308049461', 'SCI_FI'),
    ('9788308057619', 'SCI_FI'),
    ('9788308060411', 'SCI_FI'),
    ('9788373271897', 'CLASSICS'),
    ('9788375780638', 'SCI_FI'),
    ('9788375780659', 'FANTASY'),
    ('9780000000001', 'SCI_FI'),
    ('9780000000002', 'LITERARY_FICTION'),
    ('9780000000003', 'SCI_FI'),
    ('9780000000004', 'LITERARY_FICTION'),
    ('9780000000005', 'HISTORY'),
    ('9780000000006', 'CLASSICS'),
    ('9780000000007', 'LITERARY_FICTION')
) AS pairs (isbn, code)
JOIN books b ON b.isbn = pairs.isbn
JOIN genres g ON g.code = pairs.code
ON CONFLICT DO NOTHING;
