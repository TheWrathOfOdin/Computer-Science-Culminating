#This searches google images with my search querey and downloads the first x images it finds
import os
import json 
import requests 
from bs4 import BeautifulSoup 



GOOGLE_IMAGE = 'https://www.google.com/search?site=&tbm=isch&source=hp&biw=1873&bih=990&'

#pretending to be a user
usr_agent = {
    'User-Agent': 'Mozilla/5.0 (X11; Linux x86_64) AppleWebKit/537.11 (KHTML, like Gecko) Chrome/23.0.1271.64 Safari/537.11',
    'Accept': 'text/html,application/xhtml+xml,application/xml;q=0.9,*/*;q=0.8',
    'Accept-Charset': 'ISO-8859-1,utf-8;q=0.7,*;q=0.3',
    'Accept-Encoding': 'none',
    'Accept-Language': 'en-US,en;q=0.8',
    'Connection': 'keep-alive',
}

SAVE_FOLDER = 'images'

def main():
    if not os.path.exists(SAVE_FOLDER):
        os.mkdir(SAVE_FOLDER)
    download_images()

#prompts for search and how many images
def download_images():
    data = input('What are you looking for? ')
    n_images = int(input('How many images do you want? '))

    print('Start searching...')
    
    # get url query string
    searchurl = GOOGLE_IMAGE + 'q=' + data
    print(searchurl)

    # request url, without usr_agent the permission gets denied
    response = requests.get(searchurl, headers=usr_agent)
    html = response.text

    # find all divs where class='rg_meta'
    soup = BeautifulSoup(html, 'html.parser')
    results = soup.findAll('div', {'class': 'rg_meta'}, limit=n_images)
    
    # extract the link from the div tag
    imagelinks= []
    for re in results:
        text = re.text 
        text_dict= json.loads(text) 
        link = text_dict['ou']
        imagelinks.append(link)

    print(f'found {len(imagelinks)} images')
    print('Start downloading...')

    for i, imagelink in enumerate(imagelinks):
        # open image link and save as file
        response = requests.get(imagelink)
        
        imagename = SAVE_FOLDER + '/' + data + str(i+1) + '.jpg'
        with open(imagename, 'wb') as file:
            file.write(response.content)

    print('Done')
