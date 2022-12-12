<?php

namespace App\Models;

use Illuminate\Database\Eloquent\Model;
use Illuminate\Database\Eloquent\SoftDeletes;

/**
 * @property int     $Qt_id
 * @property int     $Q_id
 * @property int     $Qt_order
 * @property int     $created_at
 * @property int     $updated_at
 * @property int     $deleted_at
 * @property string  $Qt_name
 * @property boolean $Qt_multi
 * @property boolean $Qt_freetext
 */
class QTopic extends Model
{
    use SoftDeletes;
    /**
     * The database table used by the model.
     *
     * @var string
     */
    protected $table = 'q_topic';

    /**
     * The primary key for the model.
     *
     * @var string
     */
    protected $primaryKey = 'Qt_id';

    /**
     * Attributes that should be mass-assignable.
     *
     * @var array
     */
    protected $fillable = [
        'Q_id', 'Qt_name', 'Qt_order', 'Qt_multi', 'Qt_freetext', 'created_at', 'updated_at', 'deleted_at'
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
        'Qt_id' => 'int', 'Q_id' => 'int', 'Qt_name' => 'string', 'Qt_order' => 'int', 'Qt_multi' => 'boolean', 'Qt_freetext' => 'boolean', 'created_at' => 'timestamp', 'updated_at' => 'timestamp', 'deleted_at' => 'timestamp'
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
    public function eq_quest()
    {
        return $this->belongsTo(QQuest::class, 'Q_id');
    }
    public function eq_answers()
    {
        return $this->hasMany(QAnswers::class, 'Qt_id');
    }
}
