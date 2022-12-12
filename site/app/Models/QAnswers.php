<?php

namespace App\Models;

use Illuminate\Database\Eloquent\Model;
use Illuminate\Database\Eloquent\SoftDeletes;

/**
 * @property int     $Qa_id
 * @property int     $Qt_id
 * @property int     $Qa_order
 * @property int     $created_at
 * @property int     $updated_at
 * @property int     $deleted_at
 * @property string  $Qa_name
 * @property boolean $Qa_freetext
 */
class QAnswers extends Model
{
    use SoftDeletes;
    /**
     * The database table used by the model.
     *
     * @var string
     */
    protected $table = 'q_answers';

    /**
     * The primary key for the model.
     *
     * @var string
     */
    protected $primaryKey = 'Qa_id';

    /**
     * Attributes that should be mass-assignable.
     *
     * @var array
     */
    protected $fillable = [
        'Qt_id', 'Qa_name', 'Qa_order', 'Qa_freetext', 'created_at', 'updated_at', 'deleted_at'
    ];

    /**
     * The attributes excluded from the model's JSON form.
     *
     * @var array
     */
    protected $hidden = [];

    /**
     * The attributes that should be casted to native types.
     *
     * @var array
     */
    protected $casts = [
        'Qa_id' => 'int', 'Qt_id' => 'int', 'Qa_name' => 'string', 'Qa_order' => 'int', 'Qa_freetext' => 'boolean', 'created_at' => 'timestamp', 'updated_at' => 'timestamp', 'deleted_at' => 'timestamp'
    ];

    /**
     * The attributes that should be mutated to dates.
     *
     * @var array
     */
    protected $dates = [
        'created_at', 'updated_at', 'deleted_at'
    ];

    /**
     * Indicates if the model should be timestamped.
     *
     * @var boolean
     */
    public $timestamps = false;

    public static function boot()
    {
        parent::boot();

        static::creating(function ($article) {
            $article->created_at = now();
            $article->updated_at = now();
        });

        static::saving(function ($article) {
            $article->updated_at = now();
        });
    }

    // Scopes...

    // Functions ...

    // Relations ...
    public function eq_topics()
    {
        return $this->belongsTo(QTopic::class, 'Qt_id');
    }

    public function eq_votes()
    {
        return $this->hasMany(QVotes::class, 'Qa_id');
    }
}
