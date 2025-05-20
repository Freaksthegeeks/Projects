from flask import Flask, render_template, request
import pickle
import numpy as np
import pdfkit

app = Flask(__name__)
model = pickle.load(open("mental_health_model.pkl", "rb"))

recommendation_details = {
    "Self-Care": "Focus on consistent sleep, balanced lifestyle, and mindfulness. Consider light journaling or meditation.",
    "Guided Counseling": "You might benefit from a few counseling sessions to talk through ongoing stress or emotional imbalances.",
    "Regular Therapy": "Persistent issues suggest that ongoing therapy could help with structured mental wellness.",
    "Emergency Support": "Your responses indicate serious concerns. Please consult a mental health professional immediately."
}

@app.route('/')
def home():
    return render_template('index.html')

@app.route('/predict', methods=['POST'])
def predict():
    try:
        features = [float(x) for x in request.form.values()]
        features_np = np.array([features])
        prediction = model.predict(features_np)[0]
        probas = model.predict_proba(features_np)
        confidence = round(np.max(probas) * 100, 2)
        explanation = recommendation_details.get(prediction, "No additional information.")
        return render_template(
            'result.html',
            prediction=prediction,
            confidence=confidence,
            explanation=explanation,
            input_data=features  # <-- pass the input values!
        )
    except Exception as e:
        return render_template("error.html", message=str(e))

    
@app.route('/feedback', methods=['POST'])
def feedback():
    user_feedback = request.form['feedback']
    with open("feedback.txt", "a") as f:
        f.write(user_feedback + "\n---\n")
    return render_template("thankyou.html")

if __name__ == "__main__":
    app.run(debug=True, host='0.0.0.0', port=5000)
