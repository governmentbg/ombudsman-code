<?php

namespace App\Models;

use Illuminate\Database\Eloquent\Model;
use Illuminate\Database\Eloquent\SoftDeletes;

/**
 * @property int      $Str_id
 * @property int      $Mv_id
 * @property int      $Str_type
 * @property int      $created_at
 * @property int      $updated_at
 * @property int      $deleted_at
 * @property string   $Str_url
 * @property string   $Str_embed
 * @property DateTime $Str_start
 * @property DateTime $End_start
 */
class MStream extends Model
{
    use SoftDeletes;
    /**
     * The database table used by the model.
     *
     * @var string
     */
    protected $table = 'm_stream';

    /**
     * The primary key for the model.
     *
     * @var string
     */
    protected $primaryKey = 'Str_id';

    /**
     * Attributes that should be mass-assignable.
     *
     * @var array
     */
    protected $fillable = [
        'Mv_id', 'Str_type', 'Str_home', 'Str_url', 'Str_embed', 'Str_start', 'End_start', 'created_at', 'updated_at', 'deleted_at'
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
        'Str_id' => 'int', 'Mv_id' => 'int', 'Str_type' => 'int', 'Str_url' => 'string', 'Str_embed' => 'string', 'Str_start' => 'datetime', 'End_start' => 'datetime', 'created_at' => 'timestamp', 'updated_at' => 'timestamp', 'deleted_at' => 'timestamp'
    ];

    /**
     * The attributes that should be mutated to dates.
     *
     * @var array
     */
    protected $dates = [
        'Str_start', 'End_start', 'created_at', 'updated_at', 'deleted_at'
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

    public function eq_event()
    {
        return $this->belongsTo(MEvent::class, 'Mv_id');
    }
}
