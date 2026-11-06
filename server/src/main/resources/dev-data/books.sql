-- Sample books for local development: the books the app showed before it used the server.
-- Loaded by DevDataLoader on every start of ./gradlew bootRun. This file wins: books that are
-- already there get updated to what's written here.
--
-- All ISBNs are real Polish editions from Open Library (openlibrary.org), so the server can fetch
-- their covers by ISBN (CoverService) – no cover_url here, the server fills it in.
-- Wiedźmin is the ISBN of Krew elfów (tom 3 of the saga), the one the app's sample data used.
-- Page counts are approximate.
-- No semicolons inside the texts: the file is split into statements on them.

INSERT INTO books (source, isbn, title, authors, publisher, published_year, page_count, description) VALUES
    ('MANUAL', '9788308075197', 'Solaris', 'Stanisław Lem', 'Wydawnictwo Literackie', 1961, 224,
     'Stacja badawcza nad oceanem planety, który odpowiada na ludzkie pytania w sposób niemożliwy do rozszyfrowania.'),
    ('MANUAL', '9788308060377', 'Niezwyciężony', 'Stanisław Lem', 'Wydawnictwo Literackie', 1964, 240,
     'Załoga krążownika Niezwyciężony szuka zaginionego statku na planecie Regis III i trafia na chmury maleńkich maszyn, które przetrwały tam bez ludzi.'),
    ('MANUAL', '9788308033197', 'Opowieści o pilocie Pirxie', 'Stanisław Lem', 'Wydawnictwo Literackie', 1968, 320,
     'Opowiadania o drodze Pirxa od niezdarnego kadeta do doświadczonego komandora, który w kosmosie częściej mierzy się z ludźmi i maszynami niż z obcymi.'),
    ('MANUAL', '9788304027763', 'Lalka', 'Bolesław Prus', 'Ossolineum', 1890, 680,
     'Kupiec Stanisław Wokulski dorabia się majątku, żeby zdobyć arystokratkę Izabelę Łęcką. Do tego panorama Warszawy końca XIX wieku.'),
    ('MANUAL', '9788381881548', 'Problem trzech ciał', 'Cixin Liu', 'Rebis', 2008, 440,
     'W czasie rewolucji kulturalnej z Chin wysłano sygnał w kosmos. Odpowiedź przychodzi po latach i zmienia losy całej ludzkości.'),
    ('MANUAL', '9788375780659', 'Wiedźmin', 'Andrzej Sapkowski', 'SuperNOWA', 1994, 280,
     'Geralt z Rivii, łowca potworów, i księżniczka Ciri w świecie pełnym uprzedzeń, wojen i polityki.'),
    ('MANUAL', '9788308039854', 'Lód', 'Jacek Dukaj', 'Wydawnictwo Literackie', 2007, 1050,
     'Rok 1924, Warszawa wciąż skuta Lodem po wybuchu tunguskim. Benedykt Gierosławski jedzie Transsibem na Syberię szukać ojca, który podobno rozmawia z lutymi.'),
    ('MANUAL', '9788308039861', 'Bieguni', 'Olga Tokarczuk', 'Wydawnictwo Literackie', 2007, 450,
     'Powieść z luźno połączonych opowieści o podróżowaniu, ruchu i ludzkim ciele. Nagroda Nike 2008.'),
    ('MANUAL', '9788381884501', 'Kwiaty dla Algernona', 'Daniel Keyes', NULL, 1966, 300,
     'Charlie Gordon przechodzi eksperymentalną operację, która podnosi jego inteligencję. Historia opowiedziana w jego własnych raportach.'),
    ('MANUAL', '9788324707423', 'Pachnidło', 'Patrick Süskind', 'Świat Książki', 1985, 270,
     'Jean-Baptiste Grenouille rodzi się bez własnego zapachu, za to z najczulszym węchem w XVIII-wiecznej Francji i obsesją stworzenia doskonałych perfum.'),
    ('MANUAL', '9788324030804', 'Czarna Ziemia', 'Timothy Snyder', 'Znak', 2015, 500,
     'Historia Zagłady pokazana jako skutek zniszczenia państw i instytucji w Europie Wschodniej.'),
    ('MANUAL', '9788371808111', 'Mistrz i Małgorzata', 'Michaił Bułhakow', 'Prószyński i S-ka', 1967, 500,
     'Szatan Woland odwiedza Moskwę lat 30. Obok toczy się historia Mistrza, jego powieści o Poncjuszu Piłacie i kochającej go Małgorzaty.'),
    ('MANUAL', '9788374952903', 'Sto lat samotności', 'Gabriel García Márquez', 'Muza', 1967, 450,
     'Siedem pokoleń rodu Buendía w miasteczku Macondo: kronika miłości, wojen i samotności w duchu realizmu magicznego.')
ON CONFLICT (isbn) DO UPDATE SET
    title = EXCLUDED.title,
    authors = EXCLUDED.authors,
    publisher = EXCLUDED.publisher,
    published_year = EXCLUDED.published_year,
    page_count = EXCLUDED.page_count,
    description = EXCLUDED.description;

INSERT INTO book_genres (book_id, genre_id)
SELECT b.id, g.id
FROM (VALUES
    ('9788308075197', 'SCI_FI'),
    ('9788308060377', 'SCI_FI'),
    ('9788308033197', 'SCI_FI'),
    ('9788304027763', 'CLASSICS'),
    ('9788381881548', 'SCI_FI'),
    ('9788375780659', 'FANTASY'),
    ('9788308039854', 'SCI_FI'),
    ('9788308039861', 'LITERARY_FICTION'),
    ('9788381884501', 'SCI_FI'),
    ('9788324707423', 'LITERARY_FICTION'),
    ('9788324030804', 'HISTORY'),
    ('9788371808111', 'CLASSICS'),
    ('9788374952903', 'LITERARY_FICTION')
) AS pairs (isbn, code)
JOIN books b ON b.isbn = pairs.isbn
JOIN genres g ON g.code = pairs.code
ON CONFLICT DO NOTHING;
