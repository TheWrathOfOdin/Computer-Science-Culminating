#web scraper I created to find and extract images of litter for training dataset
import requests
from bs4 import BeautifulSoup
from csv import writer

#url of website I'm scraping goes in here
response = requests.get('')

#using beautifulSoup
soup = BeautifulSoup(response.text, 'html.parser')
#find all with class name 'post-preview' in website
posts = soup.find_all(class_='image-preview')

#write headers to labels.csv
with open('labels.csv', 'w') as csv_file:
    csv_writer = writer(csv_file)
    headers = ['Title', 'Link', 'Id']
    csv_writer.writerow(headers)

#write information to labels.csv
    for post in posts:
        title = post.find(class_='post-title').get_text().replace('\n', '')
        link = post.find('a')['href']
        imageId = post.select('post-Id')[0].get_text()
        csv_writer.writerow([title, link, Id])
