from isbnlib import *
from bs4 import BeautifulSoup
import requests
import numpy as np
import re


class BookLookup:
    isbn10 = None # isbn-10 format (usually the default)
    isbn13 = None # isbn-13 format
    title = "???"
    author = "Unknown Author"
    cover_url = "No Image"

    # Constructor (__init__) and toString (__str__) methods
    def __init__(self, isbn):
        """
        Constructor for BookLookup Class

        Parameters
        ----------
        self : BookLookup
            The object itself
        isbn : int
            ISBN # of the book that we're looking up
        """
        # Assign ISBN-13 and ISBN-10
        if is_isbn13(isbn):
            self.isbn13 = isbn
            self.isbn10 = to_isbn10(isbn)
        elif is_isbn10(isbn):
            self.isbn10 = isbn
            self.isbn13 = to_isbn13(isbn)

        # Get ISBN Info with isbnlib
        try:
            meta_dict = meta(str(isbn), service='goob')
            self.title = meta_dict['Title']
            self.author = str(meta_dict['Authors'])[2:-2]
            self.cover_url = cover(isbn)["thumbnail"]
        except:
            pass

    def __str__(self):
        return "\"{}\" by {}\nISBN: {}\nISBN-13: {}\nCover: {}".format(self.title,self.author,self.isbn10,self.isbn13,self.cover_url) # prints info about the book



    # User Agent methods
    def __rand_ua(self):
        """
        Generates a random user agent for web scraping

        Returns
        -------
        str
            A random user agent header from ua_list.txt
        """
        random_ua = ''
        ua_file = "ua_list.txt"
        try:
            with open(ua_file) as f:
                lines = f.readlines()
            if len(lines) >  0:
                prng = np.random.RandomState()
                index = prng.permutation(len(lines) - 1)
                idx = np.asarray(index, dtype=np.integer)[0]
                random_proxy = lines[int(idx)]
                random_ua = str(random_proxy).rstrip("\n")
        except Exception as ex:
            print('Exception in random_ua')
            print(str(ex))
        finally:
            return random_ua

    def __get_header(self):
        """
        Generates a randomized header

        Returns
        -------
        str
            The header as a dictionary of strings
        """
        user_agent = self.__rand_ua()
        headers = {
                'User-Agent': user_agent,
            }
        return headers



    # Methods to get book prices
    def amazon_price(self):
        """
        Gets the listed price of the book off Amazon

        Returns
        -------
        float
            Price of the book ($)
        """
        isbn = str(self.isbn10)

        # Query the store page
        search_url = requests.get("https://www.amazon.com/dp/" + isbn + "/",headers={"User-Agent":"Defined"}) #self.__get_header() )
        soup = BeautifulSoup(search_url.content, "lxml")

        # Grab the lowest price listed (excuse the mess)
        try:
            if soup.find("span", {"class": ["offer-price"]}) != None: # looks for span elements with offer-price class
                price = float(soup.find("span", {"class": ["offer-price"]}).get_text().lstrip('$')) # first result
                for i in soup.find_all("span", {"class": ["offer-price"]}): # for every element that we found
                    if float(i.get_text().lstrip('$')) < price: # compare prices for the lowest value
                        price = float(i.get_text().lstrip('$')) # item is converted to a floating-point value

            elif soup.find("span", {"class": ["header-price"]}) != None: # if offer-price is not found, we'll try finding header-price
                price = float(re.sub(r"[^0-9.]","",soup.find("span", {"class": ["header-price"]}).get_text())) # first result
                for i in soup.find_all("span", {"class": ["header-price"]}):
                    if float( re.sub(r"[^0-9.]","",i.get_text()) ) < price: # regex to only compare numbers that are prices
                        price = float( re.sub(r"[^0-9.]","",i.get_text())) # item is converted to a floating-point value
        except AttributeError:
            price = 0.0
        finally:
            return price

    def chegg_price(self):
        """
        Gets the rental price from Chegg (currently broken)

        Returns
        -------
        float
            Price of the book ($)
        """
        isbn = str(self.isbn10)

        # Search for the book
        search_url = requests.get("https://www.chegg.com/textbooks/" + isbn + "/",headers={"User-Agent":"Defined"})
        soup = BeautifulSoup(search_url.content, "lxml")

        # Find the first store page url
        store_page = requests.get( "https://www.chegg.com" + soup.select_one("a[href*=/textbooks/]")['href'],headers={"User-Agent":"Defined"})
        soup = BeautifulSoup(store_page.content, "lxml")

        # Grab the rental price
        print(soup.find("span", {"class": ["name"]}))
        price = float(soup.find("span", {"class": ["price"]}).get_text().lstrip('$')) # first result

        return None

    def bnn_price(self):
        """
        Gets the listed price of the book off Barnes & Noble

        Returns
        -------
        float
            Price of the book ($)
        """
        isbn = str(self.isbn13)

        # Query the store page
        search_url = requests.get("https://www.barnesandnoble.com/s/" + isbn,headers={"User-Agent":"Defined"}) #self.__get_header() )
        soup = BeautifulSoup(search_url.content, "lxml")

        # Grab the current price listed
        try:
            price = float(soup.find("span", {"class": ["current-price"]}).get_text().lstrip('$')) # first result
        except:
            price = 0.0

        return price





from sys import argv
from random import uniform
import time
def main():
    """ Main method """
    book = []

    if len(argv) > 1: # if arguments are given in the terminal
        for isbn in argv[1:]:
            book.append( BookLookup(str(isbn)) ) # instantiate and append BookLookup object(s) to list

    else: # if no extra arguments are given
        isbn_input = input("Please enter an ISBN (negative to quit): ")

        while int(isbn_input) > 0: # while the player hasn't typed a negative number to quit
            book.append(BookLookup(isbn_input)) # instantiate and append BookLookup object(s) to list
            isbn_input = input("Please enter another ISBN (negative to quit): ")

    # Append book data to a single string that we'll read using Java
    book_data = ""
    for i in range(len(book)):
        time.sleep( uniform(0.0,0.5) ) # adds a delay between requests
        book_data += book[i].title+"\n"+book[i].author+"\n"+book[i].isbn10+"\n"+book[i].cover_url+"\n"+str(book[i].amazon_price())+"\n" +str(book[i].bnn_price())+"\n"

    print(book_data)



if __name__ == '__main__':
    main()
