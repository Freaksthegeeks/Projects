import pandas as pd
from sklearn.tree import DecisionTreeClassifier,plot_tree
import pickle
import matplotlib.pyplot as plt

data = {
    "sleep":      [7.5, 4.0, 6.0, 8.0, 5.5, 6.5, 3.0, 7.0, 5.0, 9.0,
                   4.5, 6.0, 7.2, 8.5, 3.8, 6.8, 5.7, 7.3, 4.2, 8.0],
    "stress":     [3, 9, 6, 2, 7, 5, 10, 4, 8, 1,
                   9, 6, 4, 2, 10, 5, 7, 3, 8, 2],
    "anxiety":    [0, 1, 1, 0, 1, 0, 1, 0, 1, 0,
                   1, 1, 0, 0, 1, 0, 1, 0, 1, 0],
    "mood":       [0, 1, 1, 0, 1, 0, 1, 0, 1, 0,
                   1, 1, 0, 0, 1, 0, 1, 0, 1, 0],
    "work":       [6, 11, 8, 5, 9, 7, 12, 6, 10, 4,
                   11, 9, 6, 5, 12, 7, 9, 6, 10, 5],
    "support":    [0, 2, 1, 0, 1, 0, 2, 0, 1, 0,
                   2, 1, 0, 0, 2, 0, 1, 0, 2, 0],
    "recommendation": [
        "Self-Care", "Emergency Support", "Guided Counseling", "Self-Care", "Regular Therapy",
        "Self-Care", "Emergency Support", "Self-Care", "Regular Therapy", "Self-Care",
        "Emergency Support", "Guided Counseling", "Self-Care", "Self-Care", "Emergency Support",
        "Regular Therapy", "Guided Counseling", "Self-Care", "Emergency Support", "Self-Care"
    ]
}


df = pd.DataFrame(data)
X = df.drop("recommendation", axis=1)
y = df["recommendation"]

model = DecisionTreeClassifier(max_depth=4)
model.fit(X, y)
plot_tree(model, feature_names=X.columns, class_names=model.classes_, filled=True)
plt.show()

with open("mental_health_model.pkl", "wb") as f:
    pickle.dump(model, f)
