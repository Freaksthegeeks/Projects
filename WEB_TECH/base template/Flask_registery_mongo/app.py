from flask import Flask, render_template, request, url_for, redirect, session
from pymongo import MongoClient
from datetime import datetime
import bcrypt
import os
from bson import ObjectId
from sklearn.feature_extraction.text import TfidfVectorizer
from sklearn.metrics.pairwise import cosine_similarity
import numpy as np

app = Flask(__name__)

app.secret_key = os.environ.get("SECRET_KEY", "default_secret")

# #connect to your Mongo DB database
client = MongoClient("mongodb://localhost:27017")
db = client.get_database('total_records')
db2 = client.get_database('study_bot;')
registrationdata = db.register
contentdata = db2.courses
print(contentdata)
print (registrationdata)



#assign URLs to have a particular route 
@app.route("/", methods=['POST', 'GET'])
def index():
    message = ''
    
    # Redirect to logged_in if user is already in session
    if "email" in session:
        return redirect(url_for("logged_in"))
    
    if request.method == "POST":
        user = request.form.get("fullname")
        email = request.form.get("email")
        password1 = request.form.get("password1")
        password2 = request.form.get("password2")
        
        # Check if the username or email already exists
        user_found = registrationdata.find_one({"name": user})
        email_found = registrationdata.find_one({"email": email})
        
        if user_found:
            message = 'There already is a user by that name'
            return render_template('index.html', message=message)
        if email_found:
            message = 'This email already exists in database'
            return render_template('index.html', message=message)
        if password1 != password2:
            message = 'Passwords should match!'
            return render_template('index.html', message=message)
        else:
            # Hash the password and prepare the user data with timestamp
            hashed = bcrypt.hashpw(password2.encode('utf-8'), bcrypt.gensalt())
            user_input = {
                'name': user,
                'email': email,
                'password': hashed,
                'registration_time': datetime.now().strftime("%c")  # Add timestamp here
            }
            # Insert the user data into the database
            registrationdata.insert_one(user_input)
            
            # Retrieve the new user's email for confirmation
            user_data = registrationdata.find_one({"email": email})
            new_email = user_data['email']
            
            # Set session and redirect to logged_in page
            session["email"] = new_email
            return render_template('login.html', email=new_email)
    
    return render_template('index.html')

@app.route("/login", methods=["POST", "GET"])
def login():
    message = 'Please login to your account'
    if "email" in session:
        return redirect(url_for("logged_in"))

    if request.method == "POST":
        email = request.form.get("email")
        password = request.form.get("password")

        #check if email exists in database
        email_found = registrationdata.find_one({"email": email})
        if email_found:
            email_val = email_found['email']
            passwordcheck = email_found['password']
            #encode the password and check if it matches
            if bcrypt.checkpw(password.encode('utf-8'), passwordcheck):
                session["email"] = email_val
                return redirect(url_for('logged_in'))
            else:
                if "email" in session:
                    return redirect(url_for("logged_in"))
                message = 'Wrong password'
                return render_template('login.html', message=message)
        else:
            message = 'Email not found'
            return render_template('login.html', message=message)
    return render_template('login.html', message=message)

@app.route('/logged_in')
def logged_in():
    if "email" in session:
        email = session["email"]
        content = contentdata.find()
        contents = list(content)
        return render_template('logged_in.html',courses=contents)
    else:
        return redirect(url_for("login"))

@app.route("/logout", methods=["POST", "GET"])
def logout():
    if "email" in session:
        session.pop("email", None)
        return render_template("index.html")
    else:
        return render_template('index.html')
    
@app.route('/about')
def about():
    return render_template('about.html')
    
@app.route('/profile')
def profile():
    if "email" in session:
        email = session["email"]
        user_data = registrationdata.find_one({"email": email})
        return render_template('profile.html', user=user_data)
    else:
        return redirect(url_for("login"))
    


    

@app.route('/course/<course_id>')
def course_content(course_id):
        results = contentdata.find_one({ "_id" : course_id })

        return render_template('coursebase.html',weeks=results["weeks"],title=results["course_name"])




#1
@app.route('/dl')
def mlcourse():
    return render_template('course-deep-learning.html')
#2
@app.route('/nl')
def nlpcourse():
    return render_template('course-nlp.html')
#3
@app.route('/power')
def power():
    return render_template('course-powerbi.html')
#4
@app.route('/dm')
def digitalmarket():
    return render_template('course-digitalmarket.html')
#5
@app.route('/ml')
def machinelearning():
    return render_template('course-ml.html')
#6
@app.route('/cs')
def cybersecurity():
    return render_template('course-cs.html')
#7
@app.route('/dj')
def django():
    return render_template('course-dj.html')
#8
@app.route('/rj')
def reactjs():
    return render_template('course-rj.html')
#9
@app.route('/uiux')
def uiux():
    return render_template('course-uiux.html')
#10
@app.route('/sql')
def sql():
    return render_template('course-sql.html')
#11
@app.route('/java')
def java():
    return render_template('course-java.html')
#12
@app.route('/ai')
def ai():
    return render_template('course-ai.html')
#13
@app.route('/js')
def javascript():
    return render_template('course-js.html')
#14
@app.route('/qc')
def quantum():
    return render_template('course-qc.html')

@app.route('/search', methods=['POST'])
def search():
    if request.method == 'POST':
        query = request.form.get('query')

        # Fetch all course names from MongoDB
        courses = list(contentdata.find({}, {"course_name": 1, "_id": 0}))
        course_names = [course['course_name'] for course in courses]

        # Include the user's search query in the vectorization
        course_names.append(query)

        # Use TF-IDF to vectorize course names
        vectorizer = TfidfVectorizer().fit_transform(course_names)
        vectors = vectorizer.toarray()

        # Calculate cosine similarity between query and each course
        cosine_similarities = cosine_similarity(vectors[-1:], vectors[:-1]).flatten()
        top_matches_idx = np.argsort(cosine_similarities)[::-1]

        # Retrieve top 5 results with highest similarity
        top_courses = [{"course_name": course_names[i], "similarity": cosine_similarities[i]} for i in top_matches_idx[:5]]
        
        return render_template('search_results.html', query=query, results=top_courses)
    

if __name__ == "__main__":
  app.run(debug=True, host='0.0.0.0', port=5000)




