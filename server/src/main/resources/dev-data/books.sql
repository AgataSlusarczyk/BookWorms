-- Sample books for local development: the books the app showed before it used the server.
-- Loaded by DevDataLoader on every start of ./gradlew bootRun. This file wins: books that are
-- already there get updated to what's written here.
--
-- The first six ISBNs are the ones the app's sample data uses. The other seven are real Polish
-- editions from Open Library (openlibrary.org), the same ones the covers come from.
-- Covers: files in dev-data/covers, downloaded from Open Library and shrunk to 600 px high,
-- served by the server at /covers/<isbn>.jpg. For Lalka and Sto lat samotności Open Library only
-- has a scan of the title page, and it has no cover of Wiedźmin, so that one has none.
-- They're other people's covers, fine for trying the app out – not for a public release.
-- Page counts are approximate.
-- No semicolons inside the texts: the file is split into statements on them.

INSERT INTO books (source, isbn, title, authors, publisher, published_year, page_count, cover_url, description) VALUES
    ('MANUAL', '9788308049461', 'Solaris', 'Stanisław Lem', 'Wydawnictwo Literackie', 1961, 224, '/covers/9788308049461.jpg',
     'Stacja badawcza nad oceanem planety, który odpowiada na ludzkie pytania w sposób niemożliwy do rozszyfrowania.'),
    ('MANUAL', '9788308057619', 'Niezwyciężony', 'Stanisław Lem', 'Wydawnictwo Literackie', 1964, 240, '/covers/9788308057619.jpg',
     'Załoga krążownika Niezwyciężony szuka zaginionego statku na planecie Regis III i trafia na chmury maleńkich maszyn, które przetrwały tam bez ludzi.'),
    ('MANUAL', '9788308060411', 'Opowieści o pilocie Pirxie', 'Stanisław Lem', 'Wydawnictwo Literackie', 1968, 320, '/covers/9788308060411.jpg',
     'Opowiadania o drodze Pirxa od niezdarnego kadeta do doświadczonego komandora, który w kosmosie częściej mierzy się z ludźmi i maszynami niż z obcymi.'),
    ('MANUAL', '9788373271897', 'Lalka', 'Bolesław Prus', NULL, 1890, 680, '/covers/9788373271897.jpg',
     'Kupiec Stanisław Wokulski dorabia się majątku, żeby zdobyć arystokratkę Izabelę Łęcką. Do tego panorama Warszawy końca XIX wieku.'),
    ('MANUAL', '9788375780638', 'Problem trzech ciał', 'Cixin Liu', NULL, 2008, 440, '/covers/9788375780638.jpg',
     'W czasie rewolucji kulturalnej z Chin wysłano sygnał w kosmos. Odpowiedź przychodzi po latach i zmienia losy całej ludzkości.'),
    ('MANUAL', '9788375780659', 'Wiedźmin', 'Andrzej Sapkowski', 'SuperNOWA', 1994, 280, NULL,
     'Opowiadania o Geralcie z Rivii, łowcy potworów, który w świecie pełnym uprzedzeń trzyma się własnego kodeksu.'),
    ('MANUAL', '9788308039854', 'Lód', 'Jacek Dukaj', 'Wydawnictwo Literackie', 2007, 1050, '/covers/9788308039854.jpg',
     'Rok 1924, Warszawa wciąż skuta Lodem po wybuchu tunguskim. Benedykt Gierosławski jedzie Transsibem na Syberię szukać ojca, który podobno rozmawia z lutymi.'),
    ('MANUAL', '9788308039861', 'Bieguni', 'Olga Tokarczuk', 'Wydawnictwo Literackie', 2007, 450, '/covers/9788308039861.jpg',
     'Powieść z luźno połączonych opowieści o podróżowaniu, ruchu i ludzkim ciele. Nagroda Nike 2008.'),
    ('MANUAL', '9788381884501', 'Kwiaty dla Algernona', 'Daniel Keyes', NULL, 1966, 300, '/covers/9788381884501.jpg',
     'Charlie Gordon przechodzi eksperymentalną operację, która podnosi jego inteligencję. Historia opowiedziana w jego własnych raportach.'),
    ('MANUAL', '9788324707423', 'Pachnidło', 'Patrick Süskind', 'Świat Książki', 1985, 270, '/covers/9788324707423.jpg',
     'Jean-Baptiste Grenouille rodzi się bez własnego zapachu, za to z najczulszym węchem w XVIII-wiecznej Francji i obsesją stworzenia doskonałych perfum.'),
    ('MANUAL', '9788324030804', 'Czarna Ziemia', 'Timothy Snyder', 'Znak', 2015, 500, '/covers/9788324030804.jpg',
     'Historia Zagłady pokazana jako skutek zniszczenia państw i instytucji w Europie Wschodniej.'),
    ('MANUAL', '9788371808111', 'Mistrz i Małgorzata', 'Michaił Bułhakow', 'Prószyński i S-ka', 1967, 500, '/covers/9788371808111.jpg',
     'Szatan Woland odwiedza Moskwę lat 30. Obok toczy się historia Mistrza, jego powieści o Poncjuszu Piłacie i kochającej go Małgorzaty.'),
    ('MANUAL', '9788374952903', 'Sto lat samotności', 'Gabriel García Márquez', 'Muza', 1967, 450, '/covers/9788374952903.jpg',
     'Siedem pokoleń rodu Buendía w miasteczku Macondo: kronika miłości, wojen i samotności w duchu realizmu magicznego.')
ON CONFLICT (isbn) DO UPDATE SET
    title = EXCLUDED.title,
    authors = EXCLUDED.authors,
    publisher = EXCLUDED.publisher,
    published_year = EXCLUDED.published_year,
    page_count = EXCLUDED.page_count,
    cover_url = EXCLUDED.cover_url,
    description = EXCLUDED.description;

INSERT INTO book_genres (book_id, genre_id)
SELECT b.id, g.id
FROM (VALUES
    ('9788308049461', 'SCI_FI'),
    ('9788308057619', 'SCI_FI'),
    ('9788308060411', 'SCI_FI'),
    ('9788373271897', 'CLASSICS'),
    ('9788375780638', 'SCI_FI'),
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
